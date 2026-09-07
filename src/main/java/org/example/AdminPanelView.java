package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
import java.util.List;

@PageTitle("Administrare")
@Route(value = "admin", layout = MainView.class)
public class AdminPanelView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;

    private H2 titlu = new H2("Panou de administrare");

    private Div panelStatistici = new Div();
    private Div panelUtilizatori = new Div();
    private Div panelCategorii = new Div();

    private Grid<Utilizator> gridUtilizatori = new Grid<>(Utilizator.class, false);
    private Grid<Categorie> gridCategorii = new Grid<>(Categorie.class, false);

    public AdminPanelView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (this.utilizatorCurent == null) {
            return; // MainView se ocupă deja de redirect la login
        }

        if (!this.utilizatorCurent.esteAdmin()) {
            Notification.show("Nu ai acces la panoul de administrare!");
            UI.getCurrent().navigate(HomeView.class);
            return;
        }

        incarcaStatistici();
        incarcaUtilizatori();
        incarcaCategorii();
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);

        Tab tabStatistici = new Tab("Statistici");
        Tab tabUtilizatori = new Tab("Utilizatori");
        Tab tabCategorii = new Tab("Categorii");
        Tabs tabs = new Tabs(tabStatistici, tabUtilizatori, tabCategorii);

        panelStatistici.setWidthFull();
        panelUtilizatori.setWidthFull();
        panelCategorii.setWidthFull();
        panelUtilizatori.setVisible(false);
        panelCategorii.setVisible(false);

        construiestePanelUtilizatori();
        construiestePanelCategorii();

        tabs.addSelectedChangeListener(e -> {
            panelStatistici.setVisible(tabs.getSelectedTab() == tabStatistici);
            panelUtilizatori.setVisible(tabs.getSelectedTab() == tabUtilizatori);
            panelCategorii.setVisible(tabs.getSelectedTab() == tabCategorii);
        });

        this.add(titlu, tabs, panelStatistici, panelUtilizatori, panelCategorii);
        this.setSizeFull();
    }

    // ==================== STATISTICI ====================

    private void incarcaStatistici() {
        panelStatistici.removeAll();

        Long nrCumparatori = em.createQuery("SELECT COUNT(u) FROM Cumparator u", Long.class).getSingleResult();
        Long nrVanzatori = em.createQuery("SELECT COUNT(u) FROM Vanzator u", Long.class).getSingleResult();
        Long nrCereri = em.createQuery("SELECT COUNT(c) FROM Cerere c", Long.class).getSingleResult();
        Long nrOferte = em.createQuery("SELECT COUNT(o) FROM Oferta o", Long.class).getSingleResult();
        Long nrTranzactii = em.createQuery(
                "SELECT COUNT(t) FROM Tranzactie t WHERE t.statusPlata = 'finalizata'", Long.class).getSingleResult();
        Double sumaTotala = em.createQuery(
                "SELECT COALESCE(SUM(t.suma), 0.0) FROM Tranzactie t WHERE t.statusPlata = 'finalizata'",
                Double.class).getSingleResult();

        HorizontalLayout randSus = new HorizontalLayout(
                creazaCardStatistica(VaadinIcon.USERS, "Cumpărători", String.valueOf(nrCumparatori), "primary"),
                creazaCardStatistica(VaadinIcon.USER_CHECK, "Vânzători", String.valueOf(nrVanzatori), "primary"),
                creazaCardStatistica(VaadinIcon.CLIPBOARD_TEXT, "Cereri totale", String.valueOf(nrCereri), "contrast")
        );
        randSus.setWidthFull();
        randSus.setSpacing(true);

        HorizontalLayout randJos = new HorizontalLayout(
                creazaCardStatistica(VaadinIcon.PAPERPLANE, "Oferte totale", String.valueOf(nrOferte), "contrast"),
                creazaCardStatistica(VaadinIcon.CHECK_CIRCLE, "Tranzacții finalizate", String.valueOf(nrTranzactii), "success"),
                creazaCardStatistica(VaadinIcon.WALLET, "Volum tranzacționat", String.format("%.0f lei", sumaTotala), "success")
        );
        randJos.setWidthFull();
        randJos.setSpacing(true);
        randJos.getStyle().set("margin-top", "16px");

        panelStatistici.add(randSus, randJos);
    }

    private Div creazaCardStatistica(VaadinIcon iconType, String eticheta, String valoare, String culoare) {
        var icon = iconType.create();
        icon.setSize("26px");
        icon.getStyle().set("color", "var(--lumo-" + culoare + "-color)");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "var(--lumo-" + culoare + "-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "10px")
                .set("display", "inline-flex");

        H3 valoareSpan = new H3(valoare);
        valoareSpan.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XLARGE);

        Span etichetaSpan = new Span(eticheta);
        etichetaSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout textInfo = new VerticalLayout(valoareSpan, etichetaSpan);
        textInfo.setSpacing(false);
        textInfo.setPadding(false);

        HorizontalLayout continut = new HorizontalLayout(iconWrapper, textInfo);
        continut.setAlignItems(FlexComponent.Alignment.CENTER);
        continut.setSpacing(true);

        Div card = new Div(continut);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.MEDIUM);
        card.setWidthFull();
        return card;
    }

    // ==================== UTILIZATORI ====================

    private void construiestePanelUtilizatori() {
        gridUtilizatori.addColumn(Utilizator::getNume).setHeader("Nume").setAutoWidth(true);
        gridUtilizatori.addColumn(Utilizator::getEmail).setHeader("Email").setAutoWidth(true);
        gridUtilizatori.addColumn(this::determinaRol).setHeader("Rol").setAutoWidth(true);
        gridUtilizatori.addColumn(u -> u.getRating() != null ? String.format("%.1f", u.getRating()) : "-")
                .setHeader("Rating").setAutoWidth(true);
        gridUtilizatori.addColumn(u -> u.getDataInregistrare() != null
                        ? u.getDataInregistrare().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        : "-")
                .setHeader("Membru din").setAutoWidth(true);
        gridUtilizatori.addComponentColumn(this::creazaActiuniUtilizator).setHeader("Acțiuni").setAutoWidth(true);

        gridUtilizatori.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        gridUtilizatori.setWidthFull();

        Div cardGrid = new Div(gridUtilizatori);
        cardGrid.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.SMALL, LumoUtility.BoxShadow.SMALL);
        cardGrid.setWidthFull();

        panelUtilizatori.add(cardGrid);
    }

    private String determinaRol(Utilizator u) {
        if (u instanceof Vanzator) return "Vânzător";
        if (u instanceof Admin) return "Admin";
        return "Cumpărător";
    }

    private Component creazaActiuniUtilizator(Utilizator item) {
        boolean esteContulPropriu = item.getIdUtilizator().equals(utilizatorCurent.getIdUtilizator());
        boolean esteAlAlteiAdmin = item instanceof Admin;

        Button cmdSterge = new Button("Șterge cont");
        cmdSterge.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdSterge.setEnabled(!esteContulPropriu && !esteAlAlteiAdmin);
        cmdSterge.addClickListener(e -> confirmaStergereUtilizator(item));
        return cmdSterge;
    }

    private void confirmaStergereUtilizator(Utilizator item) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmare ștergere cont");
        dialog.setText("Ești sigur că vrei să ștergi contul lui \"" + item.getNume() + "\" ("
                + item.getEmail() + ")? Se vor șterge automat și toate cererile/ofertele/tranzacțiile asociate.");

        dialog.setCancelable(true);
        dialog.setCancelText("Renunță");
        dialog.setConfirmText("Șterge contul");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> stergeUtilizator(item));

        dialog.open();
    }

    private void stergeUtilizator(Utilizator item) {
        try {
            this.em.getTransaction().begin();
            this.em.remove(this.em.merge(item));
            this.em.getTransaction().commit();
            Notification.show("Cont șters cu succes!");
            incarcaUtilizatori();
            incarcaStatistici();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la ștergere: " + ex.getMessage());
        }
    }

    private void incarcaUtilizatori() {
        List<Utilizator> lst = em.createQuery("SELECT u FROM Utilizator u ORDER BY u.nume", Utilizator.class)
                .getResultList();
        gridUtilizatori.setItems(lst);
    }

    // ==================== CATEGORII ====================

    private void construiestePanelCategorii() {
        Button cmdAdaugaCategorie = new Button("Adaugă categorie", VaadinIcon.PLUS.create());
        cmdAdaugaCategorie.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        cmdAdaugaCategorie.addClickListener(e -> deschideDialogCategorie(null));

        gridCategorii.addColumn(Categorie::getNume).setHeader("Nume").setAutoWidth(true);
        gridCategorii.addColumn(Categorie::getDescriere).setHeader("Descriere").setFlexGrow(2);
        gridCategorii.addColumn(this::numarCereriAsociate).setHeader("Cereri asociate").setAutoWidth(true);
        gridCategorii.addComponentColumn(this::creazaActiuniCategorie).setHeader("Acțiuni").setAutoWidth(true);

        gridCategorii.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        gridCategorii.setWidthFull();

        Div cardGrid = new Div(gridCategorii);
        cardGrid.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.SMALL, LumoUtility.BoxShadow.SMALL);
        cardGrid.setWidthFull();

        VerticalLayout continut = new VerticalLayout(cmdAdaugaCategorie, cardGrid);
        continut.setPadding(false);

        panelCategorii.add(continut);
    }

    private String numarCereriAsociate(Categorie c) {
        Long nr = em.createQuery(
                        "SELECT COUNT(cerere) FROM Cerere cerere WHERE cerere.categorie.idCategorie = :id", Long.class)
                .setParameter("id", c.getIdCategorie())
                .getSingleResult();
        return String.valueOf(nr);
    }

    private Component creazaActiuniCategorie(Categorie item) {
        Button cmdEditeaza = new Button(VaadinIcon.EDIT.create());
        cmdEditeaza.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdEditeaza.getElement().setAttribute("title", "Editează");
        cmdEditeaza.addClickListener(e -> deschideDialogCategorie(item));

        Button cmdSterge = new Button(VaadinIcon.TRASH.create());
        cmdSterge.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdSterge.getElement().setAttribute("title", "Șterge");
        cmdSterge.addClickListener(e -> confirmaStergereCategorie(item));

        return new HorizontalLayout(cmdEditeaza, cmdSterge);
    }

    private void deschideDialogCategorie(Categorie categorieDeEditat) {
        boolean esteEditare = categorieDeEditat != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(esteEditare ? "Editează categoria" : "Categorie nouă");

        TextField nume = new TextField("Nume");
        TextField descriere = new TextField("Descriere");
        nume.setWidthFull();
        descriere.setWidthFull();

        if (esteEditare) {
            nume.setValue(categorieDeEditat.getNume() != null ? categorieDeEditat.getNume() : "");
            descriere.setValue(categorieDeEditat.getDescriere() != null ? categorieDeEditat.getDescriere() : "");
        }

        Button cmdSalveaza = new Button("Salvează");
        cmdSalveaza.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cmdSalveaza.addClickListener(e -> {
            if (nume.getValue().isBlank()) {
                Notification.show("Numele categoriei este obligatoriu!");
                return;
            }
            salveazaCategorie(esteEditare ? categorieDeEditat : null, nume.getValue(), descriere.getValue());
            dialog.close();
        });

        Button cmdAnuleaza = new Button("Renunță", e -> dialog.close());

        VerticalLayout continut = new VerticalLayout(nume, descriere,
                new HorizontalLayout(cmdSalveaza, cmdAnuleaza));
        continut.setPadding(false);
        dialog.add(continut);
        dialog.open();
    }

    private void salveazaCategorie(Categorie categorieExistenta, String nume, String descriere) {
        try {
            this.em.getTransaction().begin();
            if (categorieExistenta != null) {
                Categorie gestionata = this.em.merge(categorieExistenta);
                gestionata.setNume(nume);
                gestionata.setDescriere(descriere);
            } else {
                Categorie categorieNoua = new Categorie();
                categorieNoua.setNume(nume);
                categorieNoua.setDescriere(descriere);
                this.em.persist(categorieNoua);
            }
            this.em.getTransaction().commit();
            Notification.show("Categorie salvată cu succes!");
            incarcaCategorii();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la salvare: " + ex.getMessage());
        }
    }

    private void confirmaStergereCategorie(Categorie item) {
        Long nrCereriAsociate = em.createQuery(
                        "SELECT COUNT(c) FROM Cerere c WHERE c.categorie.idCategorie = :id", Long.class)
                .setParameter("id", item.getIdCategorie())
                .getSingleResult();

        if (nrCereriAsociate > 0) {
            Notification.show("Nu poți șterge această categorie - are " + nrCereriAsociate
                    + " cerere/cereri asociate!");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmare ștergere");
        dialog.setText("Ești sigur că vrei să ștergi categoria \"" + item.getNume() + "\"?");
        dialog.setCancelable(true);
        dialog.setCancelText("Renunță");
        dialog.setConfirmText("Șterge");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                this.em.getTransaction().begin();
                this.em.remove(this.em.merge(item));
                this.em.getTransaction().commit();
                Notification.show("Categorie ștearsă!");
                incarcaCategorii();
            } catch (Exception ex) {
                if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
                Notification.show("Eroare la ștergere: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private void incarcaCategorii() {
        List<Categorie> lst = em.createQuery("SELECT c FROM Categorie c ORDER BY c.nume", Categorie.class)
                .getResultList();
        gridCategorii.setItems(lst);
    }
}
