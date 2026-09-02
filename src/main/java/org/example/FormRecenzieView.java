package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;

import java.util.List;

@PageTitle("Lasă o recenzie")
@Route(value = "recenzie-form", layout = MainView.class)
public class FormRecenzieView extends VerticalLayout implements HasUrlParameter<Integer> {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private Tranzactie tranzactie = null;
    private Integer idEvaluat = null;
    private boolean recenzieExistenta = false;

    private H1 titluForm = new H1("Lasă o recenzie");
    private Span subtitlu = new Span();
    private ComboBox<Integer> rating = new ComboBox<>("Rating (1-5):");
    private TextArea comentariu = new TextArea("Comentariu:");
    private Button cmdTrimite = new Button("Trimite recenzie");
    private Button cmdAbandon = new Button("Renunță");

    public FormRecenzieView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        rating.setItems(1, 2, 3, 4, 5);

        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer idTranzactie) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (idTranzactie != null) {
            this.tranzactie = em.find(Tranzactie.class, idTranzactie);
        }

        determinaEvaluat();
        verificaRecenzieExistenta();
        refreshForm();
    }

    private void initViewLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(rating, comentariu);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("400px");

        HorizontalLayout actionToolbar = new HorizontalLayout(cmdTrimite, cmdAbandon);
        this.add(titluForm, subtitlu, formLayout, actionToolbar);
    }

    private void initControllerActions() {
        cmdTrimite.addClickListener(e -> trimiteRecenzie());
        cmdAbandon.addClickListener(e -> UI.getCurrent().navigate(NavigableGridCerereView.class));
    }

    private void determinaEvaluat() {
        this.idEvaluat = null;
        if (this.tranzactie == null || this.tranzactie.getOferta() == null) {
            return;
        }
        Oferta oferta = this.tranzactie.getOferta();

        if (this.utilizatorCurent != null && this.utilizatorCurent.esteCumparator()) {
            // Cumparatorul evalueaza vanzatorul
            if (oferta.getVanzator() != null) {
                this.idEvaluat = oferta.getVanzator().getIdUtilizator();
            }
        } else {
            // Vanzatorul evalueaza cumparatorul
            if (oferta.getCerere() != null && oferta.getCerere().getCumparator() != null) {
                this.idEvaluat = oferta.getCerere().getCumparator().getIdUtilizator();
            }
        }
    }

    private void verificaRecenzieExistenta() {
        this.recenzieExistenta = false;
        if (this.tranzactie == null || this.utilizatorCurent == null) {
            return;
        }
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(r) FROM Recenzie r WHERE r.tranzactie.idTranzactie = :idTranzactie "
                                    + "AND r.idEvaluator = :idEvaluator", Long.class)
                    .setParameter("idTranzactie", this.tranzactie.getIdTranzactie())
                    .setParameter("idEvaluator", this.utilizatorCurent.getIdUtilizator())
                    .getSingleResult();
            this.recenzieExistenta = count != null && count > 0;
        } catch (NoResultException ex) {
            this.recenzieExistenta = false;
        }
    }

    private void refreshForm() {
        if (this.tranzactie == null) {
            subtitlu.setText("Tranzacție invalidă!");
            setFormEnabled(false);
            return;
        }
        if (this.idEvaluat == null) {
            subtitlu.setText("Nu s-a putut determina cealaltă parte a tranzacției.");
            setFormEnabled(false);
            return;
        }
        if (this.recenzieExistenta) {
            subtitlu.setText("Ai lăsat deja o recenzie pentru această tranzacție. Mulțumim!");
            setFormEnabled(false);
            return;
        }

        subtitlu.setText("Tranzacție #" + this.tranzactie.getIdTranzactie()
                + " | Suma: " + this.tranzactie.getSuma() + " lei");
        setFormEnabled(true);
    }

    private void setFormEnabled(boolean enabled) {
        rating.setEnabled(enabled);
        comentariu.setEnabled(enabled);
        cmdTrimite.setEnabled(enabled);
    }

    private void trimiteRecenzie() {
        if (this.tranzactie == null || this.idEvaluat == null || this.utilizatorCurent == null) {
            Notification.show("Nu se poate trimite recenzia!");
            return;
        }
        if (rating.getValue() == null) {
            Notification.show("Selectează un rating!");
            return;
        }

        try {
            this.em.getTransaction().begin();

            Tranzactie tranzactieGestionata = this.em.merge(this.tranzactie);

            Recenzie recenzieNoua = new Recenzie(
                    this.utilizatorCurent.getIdUtilizator(),
                    this.idEvaluat,
                    rating.getValue(),
                    comentariu.getValue(),
                    tranzactieGestionata
            );
            tranzactieGestionata.addRecenzie(recenzieNoua);
            this.em.persist(recenzieNoua);

            // Recalculeaza rating-ul utilizatorului evaluat pe baza tuturor recenziilor primite
            List<Recenzie> recenziiPrimite = this.em.createQuery(
                            "SELECT r FROM Recenzie r WHERE r.idEvaluat = :idEvaluat", Recenzie.class)
                    .setParameter("idEvaluat", this.idEvaluat)
                    .getResultList();

            Utilizator utilizatorEvaluat = this.em.find(Utilizator.class, this.idEvaluat);
            if (utilizatorEvaluat != null) {
                utilizatorEvaluat.calculeazaRating(recenziiPrimite);
            }

            this.em.getTransaction().commit();

            Notification.show("Recenzie trimisă. Mulțumim!");
            UI.getCurrent().navigate(NavigableGridCerereView.class);
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la trimiterea recenziei: " + ex.getMessage());
        }
    }
}
