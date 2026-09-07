package org.example;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

@PageTitle("Categorii")
@Route(value = "categorii", layout = MainView.class)
public class CategoriiView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private H2 titlu = new H2("Categorii");
    private FlexLayout cardsLayout = new FlexLayout();

    public CategoriiView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        incarcaCategorii();
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);

        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.getStyle().set("gap", "16px");
        cardsLayout.setWidthFull();

        this.add(titlu, cardsLayout);
    }

    private void incarcaCategorii() {
        List<Categorie> categorii = em.createQuery("SELECT c FROM Categorie c ORDER BY c.nume", Categorie.class)
                .getResultList();

        cardsLayout.removeAll();
        for (Categorie c : categorii) {
            Long nrCereriActive = em.createQuery(
                            "SELECT COUNT(cerere) FROM Cerere cerere WHERE cerere.categorie.idCategorie = :id "
                                    + "AND cerere.status IN ('deschisa','activa')", Long.class)
                    .setParameter("id", c.getIdCategorie())
                    .getSingleResult();

            cardsLayout.add(creazaCardCategorie(c, nrCereriActive));
        }
    }

    private Div creazaCardCategorie(Categorie c, Long nrCereriActive) {
        var icon = VaadinIcon.TAGS.create();
        icon.setSize("22px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "10px")
                .set("display", "inline-flex")
                .set("margin-bottom", "8px");

        H3 nume = new H3(c.getNume());
        nume.addClassNames(LumoUtility.Margin.NONE);

        Paragraph descriere = new Paragraph(
                c.getDescriere() != null && !c.getDescriere().isBlank() ? c.getDescriere() : "-");
        descriere.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.XSMALL,
                LumoUtility.Margin.Bottom.SMALL);

        var badge = new com.vaadin.flow.component.html.Span(nrCereriActive + " cereri active");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("success");
        badge.getElement().getThemeList().add("small");
        badge.getElement().getThemeList().add("pill");

        VerticalLayout continut = new VerticalLayout(iconWrapper, nume, descriere, badge);
        continut.setSpacing(false);
        continut.setPadding(false);

        Div card = new Div(continut);
        card.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.MEDIUM, "hover-lift");
        card.setWidth("260px");

        return card;
    }
}
