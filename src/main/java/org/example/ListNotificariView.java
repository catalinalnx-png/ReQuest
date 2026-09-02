package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Notificările mele")
@Route(value = "notificari", layout = MainView.class)
public class ListNotificariView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private List<Notificare> notificari = new ArrayList<>();

    private H1 titluForm = new H1("Notificările mele");
    private Button cmdInapoi = new Button("Înapoi");
    private Grid<Notificare> grid = new Grid<>(Notificare.class);

    public ListNotificariView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        initControllerActions();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);
        refreshForm();
    }

    private void initViewLayout() {
        HorizontalLayout toolbar = new HorizontalLayout(cmdInapoi);

        grid.removeAllColumns();
        grid.addColumn(Notificare::getMesaj).setHeader("Mesaj").setFlexGrow(3);
        grid.addColumn(Notificare::getTipNotificare).setHeader("Tip");
        grid.addColumn(n -> n.getDataCreare() != null ? n.getDataCreare().toString() : "-")
                .setHeader("Data");
        grid.addColumn(n -> Boolean.TRUE.equals(n.getCitita()) ? "Citită" : "Necitită")
                .setHeader("Status");
        grid.addComponentColumn(this::createGridActionsButtons).setHeader("Acțiuni");

        this.add(titluForm, toolbar, grid);
    }

    private void initControllerActions() {
        cmdInapoi.addClickListener(e -> UI.getCurrent().navigate(MainView.class));
    }

    private Component createGridActionsButtons(Notificare item) {
        Button cmdMarcheaza = new Button("Marchează ca citită");
        cmdMarcheaza.setEnabled(!Boolean.TRUE.equals(item.getCitita()));
        cmdMarcheaza.addClickListener(e -> marcheazaCaCitita(item));
        return new HorizontalLayout(cmdMarcheaza);
    }

    private void marcheazaCaCitita(Notificare item) {
        try {
            this.em.getTransaction().begin();
            Notificare notificareGestionata = this.em.merge(item);
            notificareGestionata.marcheazaCaCitita();
            this.em.getTransaction().commit();
            refreshForm();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare: " + ex.getMessage());
        }
    }

    private void refreshForm() {
        this.notificari.clear();
        if (this.utilizatorCurent != null) {
            List<Notificare> lst = this.em.createQuery(
                            "SELECT n FROM Notificare n WHERE n.utilizator.idUtilizator = :id "
                                    + "ORDER BY n.dataCreare DESC",
                            Notificare.class)
                    .setParameter("id", this.utilizatorCurent.getIdUtilizator())
                    .getResultList();
            this.notificari.addAll(lst);
        }
        grid.setItems(this.notificari);
    }
}
