package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

@PageTitle("Tranzacțiile mele")
@Route(value = "tranzactiile-mele", layout = MainView.class)
public class TranzactiileMeleView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private List<Tranzactie> tranzactii = new ArrayList<>();

    private H2 titlu = new H2("Tranzacțiile mele");
    private Grid<Tranzactie> grid = new Grid<>(Tranzactie.class, false);

    public TranzactiileMeleView() {
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

        incarcaTranzactii();
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);

        grid.addColumn(t -> t.getOferta() != null && t.getOferta().getCerere() != null
                        ? t.getOferta().getCerere().getTitlu() : "-")
                .setHeader("Cerere").setFlexGrow(2);
        grid.addColumn(t -> t.getSuma() != null ? t.getSuma() + " lei" : "-")
                .setHeader("Sumă").setAutoWidth(true);
        grid.addColumn(this::determinaCealaltaParte).setHeader("Cu cine").setAutoWidth(true);
        grid.addComponentColumn(this::creazaStatusBadge).setHeader("Status").setAutoWidth(true);
        grid.addColumn(t -> t.getDataFinalizare() != null
                        ? t.getDataFinalizare().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        : "-")
                .setHeader("Data").setAutoWidth(true);
        grid.addComponentColumn(this::creazaActiuni).setHeader("Acțiuni").setAutoWidth(true);

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

    private String determinaCealaltaParte(Tranzactie t) {
        if (t.getOferta() == null) return "-";
        if (utilizatorCurent != null && utilizatorCurent.esteCumparator()) {
            return t.getOferta().getVanzator() != null ? t.getOferta().getVanzator().getNume() : "-";
        } else {
            return t.getOferta().getCerere() != null && t.getOferta().getCerere().getCumparator() != null
                    ? t.getOferta().getCerere().getCumparator().getNume() : "-";
        }
    }

    private Span creazaStatusBadge(Tranzactie t) {
        Span badge = new Span(t.getStatusPlata());
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("small");
        badge.getElement().getThemeList().add("pill");
        if ("finalizata".equals(t.getStatusPlata())) {
            badge.getElement().getThemeList().add("success");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private Component creazaActiuni(Tranzactie item) {
        Button cmdFactura = new Button("Vezi factura");
        cmdFactura.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdFactura.addClickListener(e -> arataFactura(item));
        return cmdFactura;
    }

    private void arataFactura(Tranzactie item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Factură");

        Paragraph text = new Paragraph(item.genereazaFactura());
        text.getStyle().set("font-family", "monospace").set("white-space", "pre-wrap");

        Button cmdInchide = new Button("Închide", e -> dialog.close());

        VerticalLayout continut = new VerticalLayout(text, cmdInchide);
        continut.setPadding(false);
        dialog.add(continut);
        dialog.open();
    }

    private void incarcaTranzactii() {
        List<Tranzactie> lst;
        if (utilizatorCurent.esteCumparator()) {
            lst = em.createQuery(
                            "SELECT t FROM Tranzactie t WHERE t.oferta.cerere.cumparator.idUtilizator = :id "
                                    + "ORDER BY t.dataFinalizare DESC", Tranzactie.class)
                    .setParameter("id", utilizatorCurent.getIdUtilizator())
                    .getResultList();
        } else if (utilizatorCurent.esteVanzator()) {
            lst = em.createQuery(
                            "SELECT t FROM Tranzactie t WHERE t.oferta.vanzator.idUtilizator = :id "
                                    + "ORDER BY t.dataFinalizare DESC", Tranzactie.class)
                    .setParameter("id", utilizatorCurent.getIdUtilizator())
                    .getResultList();
        } else {
            lst = new ArrayList<>();
        }

        this.tranzactii.clear();
        this.tranzactii.addAll(lst);
        grid.setItems(this.tranzactii);
    }
}
