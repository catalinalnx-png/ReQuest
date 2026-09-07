package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Vânzători")
@Route(value = "vanzatori", layout = MainView.class)
public class VanzatoriView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private H2 titlu = new H2("Vânzători");
    private Grid<Vanzator> grid = new Grid<>(Vanzator.class, false);

    public VanzatoriView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        incarcaVanzatori();
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);

        grid.addColumn(Vanzator::getNume).setHeader("Nume").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(this::creazaRatingBadge).setHeader("Rating").setAutoWidth(true);
        grid.addComponentColumn(this::creazaSpecializareBadges).setHeader("Specializare").setFlexGrow(2);
        grid.addColumn(v -> v.getDataInregistrare() != null
                        ? v.getDataInregistrare().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        : "-")
                .setHeader("Membru din").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setWidthFull();

        Div cardGrid = new Div(grid);
        cardGrid.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.SMALL, LumoUtility.BoxShadow.SMALL);
        cardGrid.setWidthFull();

        this.add(titlu, cardGrid);
        this.setSizeFull();
    }

    private Component creazaRatingBadge(Vanzator v) {
        HorizontalLayout layout = new HorizontalLayout();
        var iconStea = VaadinIcon.STAR.create();
        iconStea.setSize("16px");
        iconStea.getStyle().set("color", "var(--lumo-warning-color)");

        Double rating = v.getRating();
        Span text = new Span(rating != null ? String.format("%.1f", rating) : "0.0");
        layout.add(iconStea, text);
        layout.setSpacing(false);
        return layout;
    }

    private Component creazaSpecializareBadges(Vanzator v) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        if (v.getCategoriiSpecializare() == null || v.getCategoriiSpecializare().isEmpty()) {
            Span span = new Span("-");
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            layout.add(span);
        } else {
            for (Categorie c : v.getCategoriiSpecializare()) {
                Span badge = new Span(c.getNume());
                badge.getElement().getThemeList().add("badge");
                badge.getElement().getThemeList().add("contrast");
                badge.getElement().getThemeList().add("small");
                badge.getElement().getThemeList().add("pill");
                layout.add(badge);
            }
        }
        return layout;
    }

    private void incarcaVanzatori() {
        List<Vanzator> lst = em.createQuery(
                        "SELECT v FROM Vanzator v ORDER BY v.rating DESC", Vanzator.class)
                .getResultList();
        grid.setItems(lst);
    }
}
