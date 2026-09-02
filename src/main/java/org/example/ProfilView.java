package org.example;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@PageTitle("Profilul meu")
@Route(value = "profil", layout = MainView.class)
public class ProfilView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private Utilizator utilizator;

    private H2 titlu = new H2("Profilul meu");
    private TextField nume = new TextField("Nume");
    private TextField email = new TextField("Email");
    private Span rolBadge = new Span();
    private Span ratingInfo = new Span();
    private Span dataInregistrareInfo = new Span();
    private Button cmdSalveaza = new Button("Salvează modificările");

    private Div cardSpecializari;
    private MultiSelectComboBox<Categorie> specializari = new MultiSelectComboBox<>("Categorii de specializare");
    private Button cmdSalveazaSpecializari = new Button("Salvează specializările");

    private Div sectiuneRecenzii = new Div();

    public ProfilView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        initControllerActions();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (this.utilizatorCurent == null) {
            return; // MainView se ocupă de redirect la login
        }

        this.utilizator = em.find(Utilizator.class, this.utilizatorCurent.getIdUtilizator());
        refreshForm();
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);
        setMaxWidth("700px");

        // --- Card date profil ---
        nume.setWidthFull();
        email.setWidthFull();

        FormLayout formLayout = new FormLayout();
        formLayout.add(nume, email);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        rolBadge.getElement().getThemeList().add("badge");
        rolBadge.getElement().getThemeList().add("contrast");
        rolBadge.getElement().getThemeList().add("pill");

        ratingInfo.addClassNames(LumoUtility.FontWeight.MEDIUM);
        dataInregistrareInfo.addClassNames(LumoUtility.TextColor.SECONDARY);

        HorizontalLayout infoRapide = new HorizontalLayout(rolBadge, ratingInfo, dataInregistrareInfo);
        infoRapide.setSpacing(true);
        infoRapide.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        cmdSalveaza.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div cardProfil = new Div(infoRapide, formLayout, cmdSalveaza);
        cardProfil.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.LARGE,
                LumoUtility.Margin.Bottom.LARGE);
        cardProfil.setWidthFull();

        // --- Card specializări (doar pentru vânzători) ---
        specializari.setWidthFull();
        specializari.setItemLabelGenerator(Categorie::getNume);

        cmdSalveazaSpecializari.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        H3 titluSpecializari = new H3("Specializare");
        titluSpecializari.addClassNames(LumoUtility.Margin.NONE);
        Paragraph explicatieSpecializari = new Paragraph(
                "Alege categoriile pe care le acoperi ca vânzător. Acestea te ajută să găsești mai ușor cererile relevante.");
        explicatieSpecializari.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE);

        cardSpecializari = new Div(titluSpecializari, explicatieSpecializari, specializari, cmdSalveazaSpecializari);
        cardSpecializari.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.LARGE,
                LumoUtility.Margin.Bottom.LARGE);
        cardSpecializari.setWidthFull();
        cardSpecializari.setVisible(false); // vizibil doar pentru vânzători, activat în refreshForm()

        H2 titluRecenzii = new H2("Recenzii primite");
        titluRecenzii.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.SMALL);

        sectiuneRecenzii.setWidthFull();

        this.add(titlu, cardProfil, cardSpecializari, titluRecenzii, sectiuneRecenzii);
    }

    private void initControllerActions() {
        cmdSalveaza.addClickListener(e -> salveazaProfil());
        cmdSalveazaSpecializari.addClickListener(e -> salveazaSpecializari());
    }

    private void refreshForm() {
        if (this.utilizator == null) {
            return;
        }

        nume.setValue(utilizator.getNume() != null ? utilizator.getNume() : "");
        email.setValue(utilizator.getEmail() != null ? utilizator.getEmail() : "");
        rolBadge.setText(utilizatorCurent.getRol());

        Double rating = utilizator.getRating();
        ratingInfo.removeAll();
        ratingInfo.add(VaadinIcon.STAR.create());
        ratingInfo.add(new Text(" " + (rating != null ? String.format("%.1f", rating) : "0.0") + " / 5"));

        if (utilizator.getDataInregistrare() != null) {
            dataInregistrareInfo.setText("Membru din "
                    + utilizator.getDataInregistrare().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }

        // Sectiunea de specializari e vizibila doar pentru vanzatori
        if (utilizatorCurent.esteVanzator() && this.utilizator instanceof Vanzator) {
            Vanzator vanzator = (Vanzator) this.utilizator;

            List<Categorie> toateCategoriile = em.createQuery(
                    "SELECT c FROM Categorie c ORDER BY c.nume", Categorie.class).getResultList();
            specializari.setItems(toateCategoriile);
            specializari.setValue(new HashSet<>(vanzator.getCategoriiSpecializare()));

            cardSpecializari.setVisible(true);
        } else {
            cardSpecializari.setVisible(false);
        }

        incarcaRecenzii();
    }

    private void incarcaRecenzii() {
        sectiuneRecenzii.removeAll();

        List<Recenzie> recenzii = em.createQuery(
                        "SELECT r FROM Recenzie r WHERE r.idEvaluat = :id ORDER BY r.idRecenzie DESC",
                        Recenzie.class)
                .setParameter("id", utilizatorCurent.getIdUtilizator())
                .getResultList();

        if (recenzii.isEmpty()) {
            Paragraph gol = new Paragraph("Nu ai primit încă nicio recenzie.");
            gol.addClassNames(LumoUtility.TextColor.SECONDARY);
            sectiuneRecenzii.add(gol);
            return;
        }

        for (Recenzie r : recenzii) {
            Div cardRecenzie = new Div();
            cardRecenzie.addClassNames(
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.BorderRadius.MEDIUM,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.Margin.Bottom.SMALL);

            Span ratingSpan = new Span();
            ratingSpan.add(VaadinIcon.STAR.create());
            ratingSpan.add(new Text(" " + r.getRating() + " / 5"));
            ratingSpan.addClassNames(LumoUtility.FontWeight.BOLD);

            Paragraph comentariu = new Paragraph(
                    r.getComentariu() != null && !r.getComentariu().isBlank()
                            ? r.getComentariu()
                            : "(fără comentariu)");
            comentariu.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.Margin.Bottom.NONE);

            cardRecenzie.add(ratingSpan, comentariu);
            sectiuneRecenzii.add(cardRecenzie);
        }
    }

    private void salveazaProfil() {
        if (nume.getValue().isBlank() || email.getValue().isBlank()) {
            Notification.show("Numele și emailul sunt obligatorii!");
            return;
        }

        try {
            this.em.getTransaction().begin();
            Utilizator utilizatorGestionat = this.em.merge(this.utilizator);
            utilizatorGestionat.actualizeazaProfil(nume.getValue(), email.getValue());
            this.em.getTransaction().commit();

            this.utilizator = utilizatorGestionat;
            Notification.show("Profil actualizat cu succes!");
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la salvare: " + ex.getMessage());
        }
    }

    private void salveazaSpecializari() {
        if (!(this.utilizator instanceof Vanzator)) {
            return;
        }

        try {
            this.em.getTransaction().begin();
            Vanzator vanzatorGestionat = (Vanzator) this.em.merge(this.utilizator);
            vanzatorGestionat.setCategoriiSpecializare(new ArrayList<>(specializari.getValue()));
            this.em.getTransaction().commit();

            this.utilizator = vanzatorGestionat;
            Notification.show("Specializări actualizate cu succes!");
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la salvarea specializărilor: " + ex.getMessage());
        }
    }
}
