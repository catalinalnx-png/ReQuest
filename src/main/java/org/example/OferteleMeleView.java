package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Ofertele mele")
@Route(value = "ofertele-mele", layout = MainView.class)
public class OferteleMeleView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private List<Oferta> oferte = new ArrayList<>();

    private H2 titluForm = new H2("Ofertele mele");

    private TextField filterText = new TextField();
    private ComboBox<String> filterStatus = new ComboBox<>("Status");
    private Button cmdResetFiltre = new Button("Resetează filtre", VaadinIcon.CLOSE_SMALL.create());

    private Grid<Oferta> grid = new Grid<>(Oferta.class, false);

    public OferteleMeleView() {
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
            return; // MainView se ocupă deja de redirect la login
        }

        if (!this.utilizatorCurent.esteVanzator()) {
            Notification.show("Această pagină este disponibilă doar vânzătorilor!");
            UI.getCurrent().navigate(HomeView.class);
            return;
        }

        incarcaOferte();
    }

    private void initViewLayout() {
        setPadding(true);
        setSpacing(true);
        addClassNames(LumoUtility.Padding.MEDIUM);

        titluForm.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        filterText.setPlaceholder("Filtrează după titlul cererii...");
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());

        filterStatus.setItems("in_asteptare", "acceptata", "respinsa", "retrasa");
        filterStatus.setClearButtonVisible(true);

        cmdResetFiltre.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout filtreLayout = new HorizontalLayout(filterText, filterStatus, cmdResetFiltre);
        filtreLayout.setAlignItems(Alignment.END);
        filtreLayout.setSpacing(true);

        Div cardFiltre = new Div(filtreLayout);
        cardFiltre.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        cardFiltre.setWidthFull();

        grid.addColumn(o -> o.getCerere() != null ? o.getCerere().getTitlu() : "-")
                .setHeader("Cerere").setSortable(true).setFlexGrow(2);
        grid.addColumn(Oferta::getPret).setHeader("Preț").setSortable(true).setAutoWidth(true);
        grid.addColumn(Oferta::getDescriere).setHeader("Descriere").setFlexGrow(2);
        grid.addComponentColumn(this::creazaStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addColumn(o -> o.getDataTrimitere() != null
                        ? o.getDataTrimitere().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        : "-")
                .setHeader("Trimisă la").setSortable(true).setAutoWidth(true);
        grid.addColumn(o -> o.getTranzactieAsociata() != null
                        ? "Tranzacție #" + o.getTranzactieAsociata().getIdTranzactie()
                        : "-")
                .setHeader("Tranzacție").setAutoWidth(true);
        grid.addComponentColumn(this::createGridActionsButtons).setHeader("Acțiuni").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setWidthFull();

        Div cardGrid = new Div(grid);
        cardGrid.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.SMALL,
                LumoUtility.BoxShadow.SMALL);
        cardGrid.setWidthFull();

        this.add(titluForm, cardFiltre, cardGrid);
        this.setSizeFull();
    }

    private Span creazaStatusBadge(Oferta o) {
        Span badge = new Span(o.getStatus());
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("small");
        badge.getElement().getThemeList().add("pill");

        if (o.getStatus() == null) {
            return badge;
        }
        switch (o.getStatus()) {
            case "acceptata":
                badge.getElement().getThemeList().add("success");
                break;
            case "respinsa":
            case "retrasa":
                badge.getElement().getThemeList().add("error");
                break;
            case "in_asteptare":
                badge.getElement().getThemeList().add("contrast");
                break;
            default:
                break;
        }
        return badge;
    }

    private Component createGridActionsButtons(Oferta item) {
        HorizontalLayout layout = new HorizontalLayout();

        Button cmdVeziCerere = new Button("Vezi cerere", VaadinIcon.LIST.create());
        cmdVeziCerere.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdVeziCerere.addClickListener(e -> {
            if (item.getCerere() != null) {
                UI.getCurrent().navigate(ListOferteView.class, item.getCerere().getIdCerere());
            }
        });
        layout.add(cmdVeziCerere);

        if ("in_asteptare".equals(item.getStatus())) {
            Button cmdRetrage = new Button("Retrage");
            cmdRetrage.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            cmdRetrage.addClickListener(e -> retrageOferta(item));
            layout.add(cmdRetrage);
        }

        if (item.getTranzactieAsociata() != null) {
            Button cmdRecenzie = new Button("Lasă recenzie");
            cmdRecenzie.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            cmdRecenzie.addClickListener(e -> {
                UI.getCurrent().navigate(FormRecenzieView.class,
                        item.getTranzactieAsociata().getIdTranzactie());
            });
            layout.add(cmdRecenzie);
        }

        return layout;
    }

    private void retrageOferta(Oferta item) {
        boolean esteProprietarul = item.getVanzator() != null
                && item.getVanzator().getIdUtilizator().equals(this.utilizatorCurent.getIdUtilizator());

        if (!esteProprietarul) {
            Notification.show("Nu ai dreptul să retragi această ofertă!");
            return;
        }

        try {
            this.em.getTransaction().begin();

            // NOTA: nu folosim Vanzator.retrageOferta() aici, fiindca acea metoda face
            // listaOferte.remove(oferta), iar colectia are orphanRemoval=true - ar sterge
            // definitiv oferta din baza de date in loc sa-i schimbe doar statusul.
            Oferta ofertaGestionata = this.em.merge(item);
            ofertaGestionata.setStatus("retrasa");

            if (ofertaGestionata.getCerere() != null && ofertaGestionata.getCerere().getCumparator() != null) {
                Utilizator cumparatorEntity = this.em.find(
                        Utilizator.class, ofertaGestionata.getCerere().getCumparator().getIdUtilizator());
                Notificare notificare = new Notificare(
                        "Vânzătorul și-a retras oferta de " + ofertaGestionata.getPret()
                                + " lei pentru cererea \"" + ofertaGestionata.getCerere().getTitlu() + "\".",
                        "OFERTA_RETRASA",
                        LocalDateTime.now(),
                        ofertaGestionata.getIdOferta(),
                        "OFERTA"
                );
                cumparatorEntity.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();
            Notification.show("Ofertă retrasă cu succes!");
            incarcaOferte();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la retragere: " + ex.getMessage());
        }
    }

    private void initControllerActions() {
        filterText.addValueChangeListener(e -> updateList());
        filterStatus.addValueChangeListener(e -> updateList());
        cmdResetFiltre.addClickListener(e -> {
            filterText.clear();
            filterStatus.clear();
            updateList();
        });
    }

    private void incarcaOferte() {
        List<Oferta> lst = em.createQuery(
                        "SELECT o FROM Oferta o WHERE o.vanzator.idUtilizator = :id ORDER BY o.dataTrimitere DESC",
                        Oferta.class)
                .setParameter("id", utilizatorCurent.getIdUtilizator())
                .getResultList();

        this.oferte.clear();
        this.oferte.addAll(lst);
        grid.setItems(this.oferte);
    }

    private void updateList() {
        List<Oferta> lstFiltrate = this.oferte.stream()
                .filter(o -> filterText.getValue() == null || filterText.getValue().isBlank()
                        || (o.getCerere() != null && o.getCerere().getTitlu() != null
                        && o.getCerere().getTitlu().toLowerCase().contains(filterText.getValue().toLowerCase())))
                .filter(o -> filterStatus.getValue() == null || filterStatus.getValue().equals(o.getStatus()))
                .toList();
        grid.setItems(lstFiltrate);
    }
}
