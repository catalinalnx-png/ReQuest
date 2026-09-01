package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

@PageTitle("Formular Cerere")
@Route(value = "cerere-form", layout = MainView.class)
public class FormCerereView extends VerticalLayout implements HasUrlParameter<Integer> {
    private static final long serialVersionUID = 1L;

    // Model date
    private EntityManager em;
    private Cerere cerere = null;
    private Binder<Cerere> binder = new BeanValidationBinder<>(Cerere.class);

    // Componente view
    private H1 titluForm = new H1("Editare Cerere");
    private IntegerField idCerere = new IntegerField("ID Cerere:");
    private TextField titlu = new TextField("Titlu:");
    private TextField descriere = new TextField("Descriere:");
    private NumberField bugetMax = new NumberField("Buget maxim:");
    private DateTimePicker dataLimita = new DateTimePicker("Data limită:");
    private ComboBox<String> status = new ComboBox<>("Status:");
    private ComboBox<Categorie> categorie = new ComboBox<>("Categorie:");

    // Butoane acțiuni
    private Button cmdAdaugare = new Button("Adaugă");
    private Button cmdSterge = new Button("Șterge");
    private Button cmdAbandon = new Button("Abandon");
    private Button cmdSalveaza = new Button("Salvează");

    public FormCerereView() {
        initDataModel();
        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer id) {
        if (id != null && id != 999) {
            this.cerere = em.find(Cerere.class, id);
            if (this.cerere == null) {
                Notification.show("Cererea nu a fost găsită!");
                adaugaCerereNoua();
            }
        } else {
            adaugaCerereNoua();
        }
        refreshForm();
    }

    private void initDataModel() {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        status.setItems("deschisa", "inchisa", "anulata");

        // Populeaza combo-ul de categorii direct din baza de date
        List<Categorie> categorii = em.createQuery("SELECT c FROM Categorie c", Categorie.class).getResultList();
        categorie.setItems(categorii);
        categorie.setItemLabelGenerator(Categorie::getNume);

        binder.bind(idCerere, "idCerere");
        binder.bind(titlu, "titlu");
        binder.bind(descriere, "descriere");
        binder.bind(bugetMax, "bugetMax");
        binder.bind(dataLimita, "dataLimita");
        binder.bind(status, "status");
        binder.bind(categorie, "categorie");
    }

    private void initViewLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(idCerere, titlu, descriere, bugetMax, dataLimita, status, categorie);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("400px");

        idCerere.setEnabled(false);

        HorizontalLayout actionToolbar = new HorizontalLayout(cmdAdaugare, cmdSterge, cmdAbandon, cmdSalveaza);
        this.add(titluForm, formLayout, actionToolbar);
    }

    private void initControllerActions() {
        cmdAdaugare.addClickListener(e -> {
            adaugaCerereNoua();
            refreshForm();
        });

        cmdSterge.addClickListener(e -> {
            stergeCerere();
            UI.getCurrent().navigate(NavigableGridCerereView.class);
        });

        cmdAbandon.addClickListener(e -> {
            UI.getCurrent().navigate(NavigableGridCerereView.class);
        });

        cmdSalveaza.addClickListener(e -> {
            salveazaCerere();
            UI.getCurrent().navigate(NavigableGridCerereView.class, this.cerere.getIdCerere());
        });
    }

    private void refreshForm() {
        if (this.cerere != null) {
            binder.setBean(this.cerere);
        }
    }

    private void adaugaCerereNoua() {
        this.cerere = new Cerere();
        this.cerere.setTitlu("Titlu nou");
        this.cerere.setStatus("deschisa");
        this.cerere.setBugetMax(0.0);
    }

    private void stergeCerere() {
        try {
            if (this.cerere != null && this.cerere.getIdCerere() != null) {
                this.em.getTransaction().begin();
                Cerere deSters = this.em.merge(this.cerere);
                this.em.remove(deSters);
                this.em.getTransaction().commit();
                Notification.show("Cerere ștearsă cu succes!");
            }
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la ștergere: " + ex.getMessage());
        }
    }

    private void salveazaCerere() {
        try {
            this.em.getTransaction().begin();
            this.cerere = this.em.merge(this.cerere);
            this.em.getTransaction().commit();
            Notification.show("Cerere salvată!");
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la salvare: " + ex.getMessage());
        }
    }
}
