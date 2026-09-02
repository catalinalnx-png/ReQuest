package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.LocalDateTime;
import java.util.List;

@PageTitle("Trimite ofertă")
@Route(value = "oferta-form", layout = MainView.class)
public class FormOfertaView extends VerticalLayout implements HasUrlParameter<Integer> {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private Cerere cerere = null;

    private H1 titluForm = new H1("Trimite ofertă nouă");
    private Span subtitlu = new Span();
    private ComboBox<Vanzator> vanzator = new ComboBox<>("Vânzător:");
    private NumberField pret = new NumberField("Preț oferit:");
    private TextField descriere = new TextField("Descriere:");

    private Button cmdTrimite = new Button("Trimite ofertă");
    private Button cmdAbandon = new Button("Renunță");

    public FormOfertaView() {
        initDataModel();
        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer idCerere) {
        if (idCerere != null) {
            this.cerere = em.find(Cerere.class, idCerere);
        }
        refreshForm();
    }

    private void initDataModel() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        List<Vanzator> vanzatori = em.createQuery("SELECT v FROM Vanzator v", Vanzator.class).getResultList();
        vanzator.setItems(vanzatori);
        vanzator.setItemLabelGenerator(Vanzator::getNume);
    }

    private void initViewLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(vanzator, pret, descriere);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("400px");

        HorizontalLayout actionToolbar = new HorizontalLayout(cmdTrimite, cmdAbandon);
        this.add(titluForm, subtitlu, formLayout, actionToolbar);
    }

    private void initControllerActions() {
        cmdTrimite.addClickListener(e -> {
            trimiteOferta();
        });

        cmdAbandon.addClickListener(e -> {
            if (this.cerere != null) {
                UI.getCurrent().navigate(ListOferteView.class, this.cerere.getIdCerere());
            } else {
                UI.getCurrent().navigate(NavigableGridCerereView.class);
            }
        });
    }

    private void refreshForm() {
        if (this.cerere != null) {
            subtitlu.setText("Cerere: " + this.cerere.getTitlu()
                    + " (buget max: " + this.cerere.getBugetMax() + ")");
        } else {
            subtitlu.setText("Cerere invalidă!");
        }
    }

    private void trimiteOferta() {
        if (this.cerere == null) {
            Notification.show("Nu există o cerere validă pentru această ofertă!");
            return;
        }
        if (vanzator.getValue() == null) {
            Notification.show("Selectează un vânzător!");
            return;
        }
        if (pret.getValue() == null || pret.getValue() <= 0) {
            Notification.show("Introdu un preț valid!");
            return;
        }

        try {
            this.em.getTransaction().begin();

            Cerere cerereGestionata = this.em.merge(this.cerere);
            Vanzator vanzatorGestionat = this.em.merge(vanzator.getValue());

            Oferta ofertaNoua = vanzatorGestionat.trimiteOferta(
                    cerereGestionata, pret.getValue(), descriere.getValue());
            this.em.persist(ofertaNoua);

            // Notifica cumparatorul ca a primit o oferta noua la cererea lui
            if (cerereGestionata.getCumparator() != null) {
                Utilizator cumparatorEntity = this.em.find(
                        Utilizator.class, cerereGestionata.getCumparator().getIdUtilizator());
                Notificare notificare = new Notificare(
                        "Ai primit o ofertă nouă de " + pret.getValue()
                                + " lei pentru cererea \"" + cerereGestionata.getTitlu() + "\"!",
                        "OFERTA_NOUA",
                        LocalDateTime.now(),
                        ofertaNoua.getIdOferta(),
                        "OFERTA"
                );
                cumparatorEntity.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();

            Notification.show("Ofertă trimisă cu succes!");
            UI.getCurrent().navigate(ListOferteView.class, this.cerere.getIdCerere());
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la trimiterea ofertei: " + ex.getMessage());
        }
    }
}
