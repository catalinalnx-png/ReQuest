package org.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

@PageTitle("Acasă")
@Route(value = "", layout = MainView.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;

    private Div heroBanner;
    private H1 heroTitlu = new H1();
    private Paragraph heroDescriere = new Paragraph();
    private HorizontalLayout statisticiLayout = new HorizontalLayout();

    public HomeView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);
        setSpacing(true);

        construiesteHero();

        statisticiLayout.setSpacing(true);
        statisticiLayout.setWidthFull();

        add(heroBanner, statisticiLayout);
    }

    private void construiesteHero() {
        heroTitlu.getStyle()
                .set("color", "white")
                .set("margin", "0")
                .set("font-size", "2.2rem");

        heroDescriere.getStyle()
                .set("color", "rgba(255,255,255,0.9)")
                .set("margin-top", "8px")
                .set("font-size", "1.05rem");

        VerticalLayout continut = new VerticalLayout(heroTitlu, heroDescriere);
        continut.setSpacing(false);
        continut.setPadding(false);

        heroBanner = new Div(continut);
        heroBanner.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5 0%, #9333ea 100%)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "36px");
        heroBanner.setWidthFull();
        heroBanner.addClassNames("fade-in-up");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UtilizatorSesiune utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (utilizatorCurent == null) {
            return; // MainView se ocupă deja de redirect la login
        }

        heroTitlu.setText("Bine ai venit, " + utilizatorCurent.getNume() + "!");

        statisticiLayout.removeAll();

        if (utilizatorCurent.esteCumparator()) {
            heroDescriere.setText("Iată o privire de ansamblu asupra activității tale ca și cumpărător.");
            construiesteStatisticiCumparator(utilizatorCurent.getIdUtilizator());
        } else {
            heroDescriere.setText("Iată o privire de ansamblu asupra activității tale ca și vânzător.");
            construiesteStatisticiVanzator(utilizatorCurent.getIdUtilizator());
        }
    }

    private void construiesteStatisticiCumparator(Integer idUtilizator) {
        Long cereriActive = em.createQuery(
                        "SELECT COUNT(c) FROM Cerere c WHERE c.cumparator.idUtilizator = :id "
                                + "AND c.status IN ('deschisa','activa')", Long.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        Long oferteInAsteptare = em.createQuery(
                        "SELECT COUNT(o) FROM Oferta o WHERE o.cerere.cumparator.idUtilizator = :id "
                                + "AND o.status = 'in_asteptare'", Long.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        Double totalCheltuit = em.createQuery(
                        "SELECT COALESCE(SUM(t.suma), 0.0) FROM Tranzactie t "
                                + "WHERE t.oferta.cerere.cumparator.idUtilizator = :id "
                                + "AND t.statusPlata = 'finalizata'", Double.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        statisticiLayout.add(
                creazaCardStatistica(VaadinIcon.CLIPBOARD_TEXT, "Cereri active",
                        String.valueOf(cereriActive), "primary"),
                creazaCardStatistica(VaadinIcon.HOURGLASS, "Oferte în așteptare",
                        String.valueOf(oferteInAsteptare), "warning"),
                creazaCardStatistica(VaadinIcon.WALLET, "Total cheltuit",
                        String.format("%.0f lei", totalCheltuit), "success")
        );
    }

    private void construiesteStatisticiVanzator(Integer idUtilizator) {
        Long oferteActive = em.createQuery(
                        "SELECT COUNT(o) FROM Oferta o WHERE o.vanzator.idUtilizator = :id "
                                + "AND o.status = 'in_asteptare'", Long.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        Long oferteAcceptate = em.createQuery(
                        "SELECT COUNT(o) FROM Oferta o WHERE o.vanzator.idUtilizator = :id "
                                + "AND o.status = 'acceptata'", Long.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        Double totalIncasat = em.createQuery(
                        "SELECT COALESCE(SUM(t.suma), 0.0) FROM Tranzactie t "
                                + "WHERE t.oferta.vanzator.idUtilizator = :id "
                                + "AND t.statusPlata = 'finalizata'", Double.class)
                .setParameter("id", idUtilizator)
                .getSingleResult();

        statisticiLayout.add(
                creazaCardStatistica(VaadinIcon.PAPERPLANE, "Oferte active",
                        String.valueOf(oferteActive), "primary"),
                creazaCardStatistica(VaadinIcon.CHECK_CIRCLE, "Oferte acceptate",
                        String.valueOf(oferteAcceptate), "success"),
                creazaCardStatistica(VaadinIcon.WALLET, "Total încasat",
                        String.format("%.0f lei", totalIncasat), "success")
        );
    }

    private Div creazaCardStatistica(VaadinIcon iconType, String eticheta, String valoare, String culoare) {
        Icon icon = iconType.create();
        icon.setSize("28px");
        icon.getStyle().set("color", "var(--lumo-" + culoare + "-color)");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "var(--lumo-" + culoare + "-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "12px")
                .set("display", "inline-flex");

        H3 valoareSpan = new H3(valoare);
        valoareSpan.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE);

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
                LumoUtility.Padding.LARGE,
                "hover-lift", "fade-in-up");
        card.setWidthFull();

        return card;
    }
}
