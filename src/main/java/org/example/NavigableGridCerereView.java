package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.ArrayList;
import java.util.List;

// NU adăuga import org.example.Cerere; (este în același pachet)

@PageTitle("Gestiune Cereri")
@Route(value = "lista-cereri", layout = MainView.class)
public class NavigableGridCerereView extends VerticalLayout implements HasUrlParameter<Integer> {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private List<Cerere> cereri = new ArrayList<>();
    private Cerere cerereSelectata = null;

    private H2 titluForm = new H2("Lista Cereri");

    // Filtre
    private TextField filterText = new TextField();
    private ComboBox<Categorie> filterCategorie = new ComboBox<>("Categorie");
    private ComboBox<String> filterStatus = new ComboBox<>("Status");
    private NumberField filterBugetMin = new NumberField("Buget min");
    private NumberField filterBugetMax = new NumberField("Buget max");
    private Button cmdResetFiltre = new Button("Resetează filtre", VaadinIcon.CLOSE_SMALL.create());

    private Button cmdEditCerere = new Button("Editează", VaadinIcon.EDIT.create());
    private Button cmdAdaugaCerere = new Button("Adaugă cerere", VaadinIcon.PLUS.create());
    private Button cmdStergeCerere = new Button("Șterge", VaadinIcon.TRASH.create());
    private Grid<Cerere> grid = new Grid<>(Cerere.class, false);

    public NavigableGridCerereView() {
        initDataModel();
        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer id) {
        if (id != null) {
            this.cerereSelectata = em.find(Cerere.class, id);
        }
        this.refreshForm();
    }

    private void initDataModel() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        em = emf.createEntityManager();

        List<Cerere> lst = em.createQuery("SELECT c FROM Cerere c ORDER BY c.titlu", Cerere.class).getResultList();
        cereri.clear();
        cereri.addAll(lst);

        if (!lst.isEmpty()) {
            this.cerereSelectata = cereri.get(0);
        }

        List<Categorie> categorii = em.createQuery("SELECT c FROM Categorie c ORDER BY c.nume", Categorie.class)
                .getResultList();
        filterCategorie.setItems(categorii);
        filterCategorie.setItemLabelGenerator(Categorie::getNume);

        filterStatus.setItems("deschisa", "activa", "inchisa", "anulata");

        grid.setItems(this.cereri);
    }

    private void initViewLayout() {
        setPadding(true);
        setSpacing(true);
        addClassNames(LumoUtility.Padding.MEDIUM);

        titluForm.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        // --- Card filtre ---
        filterText.setPlaceholder("Filtrează după titlu...");
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());

        filterCategorie.setClearButtonVisible(true);
        filterStatus.setClearButtonVisible(true);
        filterBugetMin.setPlaceholder("Min");
        filterBugetMax.setPlaceholder("Max");
        filterBugetMin.setWidth("110px");
        filterBugetMax.setWidth("110px");

        cmdResetFiltre.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout filtreLayout = new HorizontalLayout(
                filterText, filterCategorie, filterStatus, filterBugetMin, filterBugetMax, cmdResetFiltre);
        filtreLayout.setAlignItems(Alignment.END);
        filtreLayout.setSpacing(true);

        Div cardFiltre = new Div(filtreLayout);
        cardFiltre.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM);
        cardFiltre.setWidthFull();

        // --- Toolbar acțiuni ---
        cmdEditCerere.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cmdAdaugaCerere.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        cmdStergeCerere.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout toolbar = new HorizontalLayout(cmdEditCerere, cmdAdaugaCerere, cmdStergeCerere);
        toolbar.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        // --- Grid ---
        grid.addColumn(Cerere::getTitlu).setHeader("Titlu").setSortable(true).setAutoWidth(true);
        grid.addColumn(Cerere::getDescriere).setHeader("Descriere").setFlexGrow(2);
        grid.addColumn(c -> c.getBugetMax() != null ? c.getBugetMax() + " lei" : "-")
                .setHeader("Buget Max").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(c -> creazaStatusBadge(c.getStatus()))
                .setHeader("Status").setAutoWidth(true);
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

        this.add(titluForm, cardFiltre, toolbar, cardGrid);
        this.setSizeFull();
    }

    private Span creazaStatusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("small");
        badge.getElement().getThemeList().add("pill");

        if (status == null) {
            return badge;
        }
        switch (status) {
            case "activa":
            case "deschisa":
                badge.getElement().getThemeList().add("success");
                break;
            case "inchisa":
                badge.getElement().getThemeList().add("contrast");
                break;
            case "anulata":
                badge.getElement().getThemeList().add("error");
                break;
            default:
                break;
        }
        return badge;
    }

    private void initControllerActions() {
        filterText.addValueChangeListener(e -> updateList());
        filterCategorie.addValueChangeListener(e -> updateList());
        filterStatus.addValueChangeListener(e -> updateList());
        filterBugetMin.addValueChangeListener(e -> updateList());
        filterBugetMax.addValueChangeListener(e -> updateList());

        cmdResetFiltre.addClickListener(e -> {
            filterText.clear();
            filterCategorie.clear();
            filterStatus.clear();
            filterBugetMin.clear();
            filterBugetMax.clear();
            updateList();
        });

        cmdEditCerere.addClickListener(e -> editCerere());
        cmdAdaugaCerere.addClickListener(e -> adaugaCerere());
        cmdStergeCerere.addClickListener(e -> confirmaStergere());
    }

    private Component createGridActionsButtons(Cerere item) {
        Button cmdEditItem = new Button(VaadinIcon.EDIT.create());
        cmdEditItem.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdEditItem.getElement().setAttribute("title", "Editează");
        cmdEditItem.addClickListener(e -> {
            grid.asSingleSelect().setValue(item);
            editCerere();
        });

        Button cmdVeziOferte = new Button("Vezi oferte", VaadinIcon.LIST.create());
        cmdVeziOferte.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdVeziOferte.addClickListener(e -> {
            UI.getCurrent().navigate(ListOferteView.class, item.getIdCerere());
        });

        return new HorizontalLayout(cmdEditItem, cmdVeziOferte);
    }

    private void editCerere() {
        this.cerereSelectata = this.grid.asSingleSelect().getValue();
        if (this.cerereSelectata != null) {
            UI.getCurrent().navigate(FormCerereView.class, this.cerereSelectata.getIdCerere());
        }
    }

    private void updateList() {
        List<Cerere> lstFiltrate = this.cereri.stream()
                .filter(c -> filterText.getValue() == null || filterText.getValue().isBlank()
                        || c.getTitlu().toLowerCase().contains(filterText.getValue().toLowerCase()))
                .filter(c -> filterCategorie.getValue() == null
                        || (c.getCategorie() != null
                        && c.getCategorie().getIdCategorie().equals(filterCategorie.getValue().getIdCategorie())))
                .filter(c -> filterStatus.getValue() == null
                        || filterStatus.getValue().equals(c.getStatus()))
                .filter(c -> filterBugetMin.getValue() == null
                        || (c.getBugetMax() != null && c.getBugetMax() >= filterBugetMin.getValue()))
                .filter(c -> filterBugetMax.getValue() == null
                        || (c.getBugetMax() != null && c.getBugetMax() <= filterBugetMax.getValue()))
                .toList();
        grid.setItems(lstFiltrate);
    }

    private void refreshForm() {
        grid.setItems(this.cereri);
        if (this.cerereSelectata != null) {
            grid.select(this.cerereSelectata);
        }
    }

    private void adaugaCerere() {
        UI.getCurrent().navigate(FormCerereView.class, 999);
    }

    private void confirmaStergere() {
        this.cerereSelectata = this.grid.asSingleSelect().getValue();
        if (this.cerereSelectata == null) {
            Notification.show("Selectează o cerere din listă înainte de a o șterge!");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmare ștergere");
        dialog.setText("Ești sigur că vrei să ștergi cererea \"" + this.cerereSelectata.getTitlu() + "\"? "
                + "Această acțiune este ireversibilă și va șterge și ofertele asociate.");

        dialog.setCancelable(true);
        dialog.setCancelText("Renunță");

        dialog.setConfirmText("Șterge");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> stergeCerere());

        dialog.open();
    }

    private void stergeCerere() {
        try {
            if (this.cerereSelectata != null) {
                this.em.getTransaction().begin();
                this.em.remove(this.em.merge(this.cerereSelectata));
                this.em.getTransaction().commit();
                this.cereri.remove(this.cerereSelectata);
                updateList();
                Notification.show("Cerere ștearsă cu succes!");
            }
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare: " + ex.getMessage());
        }
    }
}
