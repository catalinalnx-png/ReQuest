package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MainView extends AppLayout implements RouterLayout, BeforeEnterObserver {

    private UtilizatorSesiune utilizatorCurent;
    private boolean menuConstruit = false;
    private Span badgeNotificari;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (this.utilizatorCurent == null) {
            event.forwardTo(LoginView.class);
            return;
        }

        if (!menuConstruit) {
            setPrimarySection(Section.DRAWER);
            construiesteHeader();
            construiesteDrawer();
            menuConstruit = true;
        } else {
            actualizeazaBadgeNotificari();
        }
    }

    private void construiesteHeader() {
        DrawerToggle toggle = new DrawerToggle();

        H1 logo = new H1("ReQuest");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout stanga = new HorizontalLayout(toggle, logo);
        stanga.setAlignItems(FlexComponent.Alignment.CENTER);
        stanga.setSpacing(true);

        Button cmdNotificari = new Button(VaadinIcon.BELL.create());
        cmdNotificari.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cmdNotificari.addClickListener(e -> UI.getCurrent().navigate("notificari"));

        badgeNotificari = new Span();
        badgeNotificari.getElement().getThemeList().add("badge");
        badgeNotificari.getElement().getThemeList().add("error");
        badgeNotificari.getElement().getThemeList().add("small");
        badgeNotificari.getElement().getThemeList().add("pill");
        badgeNotificari.getStyle().set("margin-left", "-4px");

        HorizontalLayout notifWrapper = new HorizontalLayout(cmdNotificari, badgeNotificari);
        notifWrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        notifWrapper.setSpacing(false);

        Avatar avatar = new Avatar(utilizatorCurent.getNume());

        Span numeSpan = new Span(utilizatorCurent.getNume());
        numeSpan.addClassNames(LumoUtility.FontWeight.MEDIUM);

        Span rolBadge = new Span(utilizatorCurent.getRol());
        rolBadge.getElement().getThemeList().add("badge");
        rolBadge.getElement().getThemeList().add("contrast");
        rolBadge.getElement().getThemeList().add("small");
        rolBadge.getElement().getThemeList().add("pill");

        Button cmdLogout = new Button(VaadinIcon.SIGN_OUT.create());
        cmdLogout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        cmdLogout.getElement().setAttribute("title", "Deconectare");
        cmdLogout.addClickListener(e -> {
            VaadinSession.getCurrent().setAttribute(UtilizatorSesiune.class, null);
            UI.getCurrent().navigate(LoginView.class);
        });

        HorizontalLayout dreapta = new HorizontalLayout(notifWrapper, avatar, numeSpan, rolBadge, cmdLogout);
        dreapta.setAlignItems(FlexComponent.Alignment.CENTER);
        dreapta.setSpacing(true);
        dreapta.getStyle().set("margin-right", "var(--lumo-space-m)");

        HorizontalLayout header = new HorizontalLayout(stanga, dreapta);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.getStyle().set("padding", "var(--lumo-space-s) 0");

        addToNavbar(header);

        actualizeazaBadgeNotificari();
    }

    private void construiesteDrawer() {
        SideNav nav = new SideNav();

        SideNavItem homeItem = new SideNavItem("Acasă", HomeView.class, VaadinIcon.HOME.create());
        nav.addItem(homeItem);

        if (utilizatorCurent.esteCumparator()) {
            SideNavItem cereriItem = new SideNavItem("Cererile mele");
            cereriItem.setPrefixComponent(VaadinIcon.CLIPBOARD.create());
            cereriItem.addItem(new SideNavItem("Lista Cereri", "lista-cereri", VaadinIcon.LIST.create()));
            cereriItem.addItem(new SideNavItem("Adaugă cerere nouă", "cerere-form/999", VaadinIcon.PLUS.create()));
            cereriItem.setExpanded(true);
            nav.addItem(cereriItem);
        }

        if (utilizatorCurent.esteVanzator()) {
            SideNavItem pietaItem = new SideNavItem("Piață");
            pietaItem.setPrefixComponent(VaadinIcon.CART.create());
            pietaItem.addItem(new SideNavItem("Cereri disponibile", "lista-cereri", VaadinIcon.LIST.create()));
            pietaItem.setExpanded(true);
            nav.addItem(pietaItem);
        }

        SideNavItem notifItem = new SideNavItem("Notificări", "notificari", VaadinIcon.BELL.create());
        nav.addItem(notifItem);

        nav.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        addToDrawer(nav);
    }

    private void actualizeazaBadgeNotificari() {
        int numarNecitite = numarNotificariNecitite();
        if (badgeNotificari != null) {
            badgeNotificari.setText(String.valueOf(numarNecitite));
            badgeNotificari.setVisible(numarNecitite > 0);
        }
    }

    private int numarNotificariNecitite() {
        if (utilizatorCurent == null) {
            return 0;
        }
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
            EntityManager em = emf.createEntityManager();
            Long count = em.createQuery(
                            "SELECT COUNT(n) FROM Notificare n WHERE n.utilizator.idUtilizator = :id "
                                    + "AND n.citita = false", Long.class)
                    .setParameter("id", utilizatorCurent.getIdUtilizator())
                    .getSingleResult();
            em.close();
            return count != null ? count.intValue() : 0;
        } catch (Exception ex) {
            return 0;
        }
    }

    public UtilizatorSesiune getUtilizatorCurent() {
        return utilizatorCurent;
    }
}
