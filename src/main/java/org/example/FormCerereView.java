package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
    private UtilizatorSesiune utilizatorCurent;
    private Binder<Cerere> binder = new Binder<>(Cerere.class);

    // Componente view
    private IntegerField idCerere = new IntegerField("ID Cerere");
    private TextField titlu = new TextField("Titlu");
    private TextField descriere = new TextField("Descriere");
    private NumberField bugetMax = new NumberField("Buget maxim");
    private DateTimePicker dataLimita = new DateTimePicker("Data limită");
    private ComboBox<String> status = new ComboBox<>("Status");
    private ComboBox<Categorie> categorie = new ComboBox<>("Categorie");

    // Butoane acțiuni
    private Button cmdAdaugare = new Button("Adaugă", VaadinIcon.PLUS.create());
    private Button cmdSterge = new Button("Șterge", VaadinIcon.TRASH.create());
    private Button cmdAbandon = new Button("Abandon");
    private Button cmdSalveaza = new Button("Salvează", VaadinIcon.CHECK.create());

    public FormCerereView() {
        initDataModel();
        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer id) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (this.utilizatorCurent == null || !this.utilizatorCurent.esteCumparator()) {
            Notification.show("Doar cumpărătorii pot crea sau edita cereri!");
            UI.getCurrent().navigate(NavigableGridCerereView.class);
            return;
        }

        if (id != null && id != 999) {
            this.cerere = em.find(Cerere.class, id);
            if (this.cerere == null) {
                Notification.show("Cererea nu a fost găsită!");
                adaugaCerereNoua();
            } else if (!apartineUtilizatorului(this.cerere)) {
                Notification.show("Nu ai dreptul să editezi această cerere!");
                UI.getCurrent().navigate(NavigableGridCerereView.class);
                return;
            }
        } else {
            adaugaCerereNoua();
        }
        refreshForm();
    }

    private boolean apartineUtilizatorului(Cerere c) {
        return c.getCumparator() != null
                && c.getCumparator().getIdUtilizator() != null
                && c.getCumparator().getIdUtilizator().equals(this.utilizatorCurent.getIdUtilizator());
    }

    private void initDataModel() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        status.setItems("deschisa", "activa", "inchisa", "anulata");

        List<Categorie> categorii = em.createQuery("SELECT c FROM Categorie c ORDER BY c.nume", Categorie.class)
                .getResultList();
        categorie.setItems(categorii);
        categorie.setItemLabelGenerator(Categorie::getNume);

        binder.forField(titlu)
                .asRequired("Titlul este obligatoriu")
                .bind(Cerere::getTitlu, Cerere::setTitlu);

        binder.forField(bugetMax)
                .withValidator(valoare -> valoare != null && valoare > 0,
                        "Bugetul maxim trebuie să fie un număr pozitiv")
                .bind(Cerere::getBugetMax, Cerere::setBugetMax);

        binder.bind(idCerere, "idCerere");
        binder.bind(descriere, "descriere");
        binder.bind(dataLimita, "dataLimita");
        binder.bind(status, "status");
        binder.bind(categorie, "categorie");
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);
        setMaxWidth("640px");

        // --- Header cu iconiță ---
        Icon iconHeader = VaadinIcon.CLIPBOARD_TEXT.create();
        iconHeader.setSize("22px");
        iconHeader.getStyle().set("color", "white");

        Div iconWrapper = new Div(iconHeader);
        iconWrapper.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5, #9333ea)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "12px")
                .set("display", "inline-flex");

        H2 titluForm = new H2("Editare cerere");
        titluForm.addClassNames(LumoUtility.Margin.NONE);

        HorizontalLayout headerLayout = new HorizontalLayout(iconWrapper, titluForm);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        headerLayout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        // --- Iconițe pe câmpuri ---
        idCerere.setPrefixComponent(VaadinIcon.HASH.create());
        idCerere.setEnabled(false);

        titlu.setPrefixComponent(VaadinIcon.EDIT.create());
        descriere.setPrefixComponent(VaadinIcon.FILE_TEXT.create());
        bugetMax.setPrefixComponent(VaadinIcon.WALLET.create());
        status.setPrefixComponent(VaadinIcon.FLAG.create());
        categorie.setPrefixComponent(VaadinIcon.TAGS.create());

        FormLayout formLayout = new FormLayout();
        formLayout.add(idCerere, titlu, descriere, bugetMax, dataLimita, status, categorie);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // --- Butoane colorate ---
        cmdAdaugare.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cmdSterge.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        cmdAbandon.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cmdSalveaza.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout actionToolbar = new HorizontalLayout(cmdAdaugare, cmdSterge, cmdAbandon, cmdSalveaza);
        actionToolbar.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout continutCard = new VerticalLayout(formLayout, actionToolbar);
        continutCard.setSpacing(true);
        continutCard.setPadding(false);

        Div card = new Div(continutCard);
        card.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, "fade-in-up");
        card.setWidthFull();

        this.add(headerLayout, card);
    }

    private void initControllerActions() {
        cmdAdaugare.addClickListener(e -> {
            adaugaCerereNoua();
            refreshForm();
        });

        cmdSterge.addClickListener(e -> confirmaStergere());

        cmdAbandon.addClickListener(e -> UI.getCurrent().navigate(NavigableGridCerereView.class));

        cmdSalveaza.addClickListener(e -> {
            if (valideazaFormular()) {
                salveazaCerere();
                UI.getCurrent().navigate(NavigableGridCerereView.class, this.cerere.getIdCerere());
            }
        });
    }

    private boolean valideazaFormular() {
        binder.validate();
        if (!binder.isValid()) {
            Notification.show("Formularul conține erori. Verifică titlul și bugetul maxim.");
            return false;
        }
        return true;
    }

    private void confirmaStergere() {
        if (this.cerere == null || this.cerere.getIdCerere() == null) {
            Notification.show("Nu există o cerere de șters!");
            return;
        }
        if (!apartineUtilizatorului(this.cerere)) {
            Notification.show("Nu ai dreptul să ștergi această cerere!");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmare ștergere");
        dialog.setText("Ești sigur că vrei să ștergi cererea \"" + this.cerere.getTitlu() + "\"? "
                + "Această acțiune este ireversibilă și va șterge și ofertele asociate.");

        dialog.setCancelable(true);
        dialog.setCancelText("Renunță");

        dialog.setConfirmText("Șterge");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            stergeCerere();
            UI.getCurrent().navigate(NavigableGridCerereView.class);
        });

        dialog.open();
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

        if (this.utilizatorCurent != null) {
            Cumparator cumparatorEntity = em.find(Cumparator.class, this.utilizatorCurent.getIdUtilizator());
            this.cerere.setCumparator(cumparatorEntity);
        }
    }

    private void stergeCerere() {
        if (!apartineUtilizatorului(this.cerere)) {
            Notification.show("Nu ai dreptul să ștergi această cerere!");
            return;
        }
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
        if (!apartineUtilizatorului(this.cerere)) {
            Notification.show("Nu ai dreptul să modifici această cerere!");
            return;
        }

        boolean esteCerereNoua = this.cerere.getIdCerere() == null;

        try {
            this.em.getTransaction().begin();
            this.cerere = this.em.merge(this.cerere);

            if (esteCerereNoua) {
                notificaVanzatoriiSpecializati(this.cerere);
            }

            this.em.getTransaction().commit();
            Notification.show("Cerere salvată!");
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la salvare: " + ex.getMessage());
        }
    }

    // AUTO-NOTIFICARE: la o cerere noua, anunta toti vanzatorii specializati pe categoria ei
    private void notificaVanzatoriiSpecializati(Cerere cerereNoua) {
        if (cerereNoua.getCategorie() == null) {
            return;
        }

        List<Vanzator> vanzatoriSpecializati = em.createQuery(
                        "SELECT v FROM Vanzator v JOIN v.categoriiSpecializare cat "
                                + "WHERE cat.idCategorie = :idCategorie", Vanzator.class)
                .setParameter("idCategorie", cerereNoua.getCategorie().getIdCategorie())
                .getResultList();

        for (Vanzator vanzator : vanzatoriSpecializati) {
            Notificare notificare = new Notificare(
                    "O cerere nouă în domeniul tău (" + cerereNoua.getCategorie().getNume() + "): \""
                            + cerereNoua.getTitlu() + "\"! Verifică Piața pentru detalii.",
                    "CERERE_NOUA_SPECIALIZARE",
                    java.time.LocalDateTime.now(),
                    cerereNoua.getIdCerere(),
                    "CERERE"
            );
            vanzator.adaugaNotificare(notificare);
            em.persist(notificare);
        }
    }
}