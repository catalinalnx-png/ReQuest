package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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

    private H1 titluForm = new H1("Lista Cereri");
    private TextField filterText = new TextField();
    private Button cmdEditCerere = new Button("Editează cerere...");
    private Button cmdAdaugaCerere = new Button("Adaugă cerere...");
    private Button cmdStergeCerere = new Button("Șterge cerere");
    private Grid<Cerere> grid = new Grid<>(Cerere.class);

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

        grid.setItems(this.cereri);
    }

    private void initViewLayout() {
        filterText.setPlaceholder("Filtrează după titlu...");
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        HorizontalLayout toolbar = new HorizontalLayout(filterText, cmdEditCerere, cmdAdaugaCerere, cmdStergeCerere);

        grid.setColumns("titlu", "descriere", "bugetMax", "status");
        grid.addComponentColumn(item -> createGridActionsButtons(item)).setHeader("Acțiuni");

        this.add(titluForm, toolbar, grid);
    }

    private void initControllerActions() {
        filterText.addValueChangeListener(e -> updateList());
        cmdEditCerere.addClickListener(e -> editCerere());
        cmdAdaugaCerere.addClickListener(e -> adaugaCerere());
        cmdStergeCerere.addClickListener(e -> stergeCerere());
    }

    private Component createGridActionsButtons(Cerere item) {
        Button cmdEditItem = new Button("Edit");
        cmdEditItem.addClickListener(e -> {
            grid.asSingleSelect().setValue(item);
            editCerere();
        });
        return new HorizontalLayout(cmdEditItem);
    }

    private void editCerere() {
        this.cerereSelectata = this.grid.asSingleSelect().getValue();
        if (this.cerereSelectata != null) {
            // Dacă aici e roșu, înseamnă că Cerere.java nu e salvat sau compilat
            UI.getCurrent().navigate(FormCerereView.class, this.cerereSelectata.getIdCerere());
        }
    }

    private void updateList() {
        if (filterText.getValue() != null) {
            List<Cerere> lstFiltrate = this.cereri.stream()
                    .filter(c -> c.getTitlu().toLowerCase().contains(filterText.getValue().toLowerCase()))
                    .toList();
            grid.setItems(lstFiltrate);
        }
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

    private void stergeCerere() {
        try {
            this.cerereSelectata = this.grid.asSingleSelect().getValue();
            if (this.cerereSelectata != null) {
                this.em.getTransaction().begin();
                this.em.remove(this.em.merge(this.cerereSelectata));
                this.em.getTransaction().commit();
                this.cereri.remove(this.cerereSelectata);
            }
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare: " + ex.getMessage());
        }
    }
}
