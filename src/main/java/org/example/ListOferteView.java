package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Oferte primite")
@Route(value = "lista-oferte", layout = MainView.class)
public class ListOferteView extends VerticalLayout implements HasUrlParameter<Integer> {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private Cerere cerere = null;
    private List<Oferta> oferte = new ArrayList<>();

    private H1 titluForm = new H1("Oferte primite");
    private Span subtitlu = new Span();
    private Button cmdInapoi = new Button("Înapoi la cereri");
    private Button cmdAdaugaOferta = new Button("Adaugă ofertă...");
    private Grid<Oferta> grid = new Grid<>(Oferta.class);

    public ListOferteView() {
        initDataModel();
        initViewLayout();
        initControllerActions();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer idCerere) {
        if (idCerere != null) {
            this.cerere = em.find(Cerere.class, idCerere);
        }
        refreshForm();
    }

    private void initDataModel() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();
    }

    private void initViewLayout() {
        HorizontalLayout toolbar = new HorizontalLayout(cmdInapoi, cmdAdaugaOferta);

        grid.removeAllColumns();
        grid.addColumn(Oferta::getPret).setHeader("Preț");
        grid.addColumn(Oferta::getDescriere).setHeader("Descriere");
        grid.addColumn(Oferta::getStatus).setHeader("Status");
        grid.addColumn(o -> o.getVanzator() != null ? o.getVanzator().toString() : "-")
                .setHeader("Vânzător");
        grid.addColumn(o -> o.getTranzactieAsociata() != null
                        ? "Tranzacție #" + o.getTranzactieAsociata().getIdTranzactie()
                        : "-")
                .setHeader("Tranzacție");
        grid.addComponentColumn(this::createGridActionsButtons).setHeader("Acțiuni");

        this.add(titluForm, subtitlu, toolbar, grid);
    }

    private void initControllerActions() {
        cmdInapoi.addClickListener(e -> UI.getCurrent().navigate(NavigableGridCerereView.class));

        cmdAdaugaOferta.addClickListener(e -> {
            if (this.cerere != null) {
                UI.getCurrent().navigate(FormOfertaView.class, this.cerere.getIdCerere());
            } else {
                Notification.show("Nu există o cerere selectată!");
            }
        });
    }

    private Component createGridActionsButtons(Oferta item) {
        Button cmdAccepta = new Button("Acceptă");
        cmdAccepta.addClickListener(e -> acceptaOferta(item));
        cmdAccepta.setEnabled("in_asteptare".equals(item.getStatus()));

        Button cmdRespinge = new Button("Respinge");
        cmdRespinge.addClickListener(e -> respingeOferta(item));
        cmdRespinge.setEnabled("in_asteptare".equals(item.getStatus()));

        Button cmdRecenzie = new Button("Lasă recenzie");
        cmdRecenzie.setVisible(item.getTranzactieAsociata() != null);
        cmdRecenzie.addClickListener(e -> {
            if (item.getTranzactieAsociata() != null) {
                UI.getCurrent().navigate(FormRecenzieView.class,
                        item.getTranzactieAsociata().getIdTranzactie());
            }
        });

        return new HorizontalLayout(cmdAccepta, cmdRespinge, cmdRecenzie);
    }

    private void acceptaOferta(Oferta item) {
        try {
            this.em.getTransaction().begin();

            Oferta ofertaGestionata = this.em.merge(item);

            // 1. Marcheaza oferta ca acceptata si inchide cererea asociata
            ofertaGestionata.accepta();

            // 2. Creeaza tranzactia asociata ofertei acceptate
            Tranzactie tranzactie = new Tranzactie(
                    LocalDateTime.now(),
                    "finalizata",
                    ofertaGestionata.getPret(),
                    ofertaGestionata
            );
            ofertaGestionata.setTranzactieAsociata(tranzactie);
            this.em.persist(tranzactie);

            this.em.merge(ofertaGestionata.getCerere());

            // 3. Notifica vanzatorul ca oferta lui a fost acceptata
            if (ofertaGestionata.getVanzator() != null) {
                Utilizator vanzatorEntity = this.em.find(
                        Utilizator.class, ofertaGestionata.getVanzator().getIdUtilizator());
                Notificare notificare = new Notificare(
                        "Oferta ta de " + ofertaGestionata.getPret() + " lei pentru cererea \""
                                + ofertaGestionata.getCerere().getTitlu() + "\" a fost acceptată!",
                        "OFERTA_ACCEPTATA",
                        LocalDateTime.now(),
                        ofertaGestionata.getIdOferta(),
                        "OFERTA"
                );
                vanzatorEntity.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();

            Notification.show("Ofertă acceptată! " + tranzactie.genereazaFactura());
            reloadOferte();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la acceptare: " + ex.getMessage());
        }
    }

    private void respingeOferta(Oferta item) {
        try {
            this.em.getTransaction().begin();
            Oferta ofertaGestionata = this.em.merge(item);
            ofertaGestionata.respinge();

            // Notifica vanzatorul ca oferta lui a fost respinsa
            if (ofertaGestionata.getVanzator() != null) {
                Utilizator vanzatorEntity = this.em.find(
                        Utilizator.class, ofertaGestionata.getVanzator().getIdUtilizator());
                Notificare notificare = new Notificare(
                        "Oferta ta de " + ofertaGestionata.getPret() + " lei pentru cererea \""
                                + ofertaGestionata.getCerere().getTitlu() + "\" a fost respinsă.",
                        "OFERTA_RESPINSA",
                        LocalDateTime.now(),
                        ofertaGestionata.getIdOferta(),
                        "OFERTA"
                );
                vanzatorEntity.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();
            Notification.show("Ofertă respinsă!");
            reloadOferte();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la respingere: " + ex.getMessage());
        }
    }

    private void reloadOferte() {
        this.em.clear();
        if (this.cerere != null) {
            this.cerere = this.em.find(Cerere.class, this.cerere.getIdCerere());
        }
        refreshForm();
    }

    private void refreshForm() {
        this.oferte.clear();
        if (this.cerere != null) {
            subtitlu.setText("Cerere: " + this.cerere.getTitlu());
            List<Oferta> lst = this.em.createQuery(
                            "SELECT o FROM Oferta o WHERE o.cerere.idCerere = :idCerere ORDER BY o.pret",
                            Oferta.class)
                    .setParameter("idCerere", this.cerere.getIdCerere())
                    .getResultList();
            this.oferte.addAll(lst);
        } else {
            subtitlu.setText("Nicio cerere selectată.");
        }
        grid.setItems(this.oferte);
    }
}
