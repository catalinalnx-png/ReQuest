package org.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
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
    private UtilizatorSesiune utilizatorCurent;
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
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (idCerere != null) {
            this.cerere = em.find(Cerere.class, idCerere);
        }

        cmdAdaugaOferta.setVisible(this.utilizatorCurent != null && this.utilizatorCurent.esteVanzator());

        refreshForm();
    }

    private void initDataModel() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();
    }

    private void initViewLayout() {
        HorizontalLayout toolbar = new HorizontalLayout(cmdInapoi, cmdAdaugaOferta);

        grid.removeAllColumns();
        grid.addColumn(this::formateazaPret).setHeader("Preț").setAutoWidth(true);
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

    private String formateazaPret(Oferta o) {
        if (o.getPretNegociat() != null) {
            String propunator = "CUMPARATOR".equals(o.getNegociatDe()) ? "cumpărător" : "vânzător";
            return o.getPret() + " lei → propunere: " + o.getPretNegociat()
                    + " lei (de la " + propunator + ", runda " + o.getNumarRundeNegociere() + ")";
        }
        return o.getPret() + " lei";
    }

    private void initControllerActions() {
        cmdInapoi.addClickListener(e -> UI.getCurrent().navigate(NavigableGridCerereView.class));

        cmdAdaugaOferta.addClickListener(e -> {
            if (this.utilizatorCurent == null || !this.utilizatorCurent.esteVanzator()) {
                Notification.show("Doar vânzătorii pot trimite oferte!");
                return;
            }
            if (this.cerere != null) {
                UI.getCurrent().navigate(FormOfertaView.class, this.cerere.getIdCerere());
            } else {
                Notification.show("Nu există o cerere selectată!");
            }
        });
    }

    private boolean poateGestionaOferte() {
        // Doar cumparatorul care detine cererea poate raspunde din pozitia de cumparator
        return this.utilizatorCurent != null
                && this.utilizatorCurent.esteCumparator()
                && this.cerere != null
                && this.cerere.getCumparator() != null
                && this.cerere.getCumparator().getIdUtilizator() != null
                && this.cerere.getCumparator().getIdUtilizator().equals(this.utilizatorCurent.getIdUtilizator());
    }

    private boolean esteVanzatorulOfertei(Oferta item) {
        return this.utilizatorCurent != null
                && this.utilizatorCurent.esteVanzator()
                && item.getVanzator() != null
                && item.getVanzator().getIdUtilizator().equals(this.utilizatorCurent.getIdUtilizator());
    }

    // NEGOCIERE: determina daca e randul utilizatorului curent sa raspunda la aceasta oferta
    private boolean poateRaspundeLaNegociere(Oferta item) {
        if (!"in_asteptare".equals(item.getStatus())) {
            return false;
        }
        boolean esteCumparatorProprietar = poateGestionaOferte();
        boolean esteVanzatorProprietar = esteVanzatorulOfertei(item);

        if (item.getPretNegociat() == null) {
            // Nicio negociere activa: doar cumparatorul raspunde la oferta initiala
            return esteCumparatorProprietar;
        }
        if ("CUMPARATOR".equals(item.getNegociatDe())) {
            // Cumparatorul a propus ultimul - e randul vanzatorului
            return esteVanzatorProprietar;
        } else {
            // Vanzatorul a propus ultimul - e randul cumparatorului
            return esteCumparatorProprietar;
        }
    }

    private boolean poateLasaRecenzie(Oferta item) {
        if (this.utilizatorCurent == null || item.getTranzactieAsociata() == null) {
            return false;
        }
        boolean esteVanzatorulOfertei = esteVanzatorulOfertei(item);
        boolean esteCumparatorulCererii = item.getCerere() != null
                && item.getCerere().getCumparator() != null
                && item.getCerere().getCumparator().getIdUtilizator().equals(this.utilizatorCurent.getIdUtilizator());
        return esteVanzatorulOfertei || esteCumparatorulCererii;
    }

    private Component createGridActionsButtons(Oferta item) {
        HorizontalLayout layout = new HorizontalLayout();

        boolean parteImplicata = poateGestionaOferte() || esteVanzatorulOfertei(item);

        if (parteImplicata && "in_asteptare".equals(item.getStatus())) {
            boolean randulMeu = poateRaspundeLaNegociere(item);

            String etichetaAccepta = item.getPretNegociat() != null
                    ? "Acceptă (" + item.getPretNegociat() + " lei)"
                    : "Acceptă";
            Button cmdAccepta = new Button(etichetaAccepta);
            cmdAccepta.setEnabled(randulMeu);
            cmdAccepta.addClickListener(e -> acceptaOferta(item));

            Button cmdRespinge = new Button("Respinge");
            cmdRespinge.setEnabled(randulMeu);
            cmdRespinge.addClickListener(e -> respingeOferta(item));

            String initiator = poateGestionaOferte() ? "CUMPARATOR" : "VANZATOR";
            Button cmdContraOferta = new Button("Contra-ofertă");
            cmdContraOferta.setEnabled(randulMeu);
            cmdContraOferta.addClickListener(e -> deschideDialogContraOferta(item, initiator));

            layout.add(cmdAccepta, cmdRespinge, cmdContraOferta);

            if (!randulMeu) {
                Span astept = new Span("Aștepți răspunsul celeilalte părți");
                astept.getElement().getThemeList().add("badge");
                astept.getElement().getThemeList().add("contrast");
                astept.getElement().getThemeList().add("small");
                layout.add(astept);
            }
        }

        if (poateLasaRecenzie(item)) {
            Button cmdRecenzie = new Button("Lasă recenzie");
            cmdRecenzie.addClickListener(e -> {
                UI.getCurrent().navigate(FormRecenzieView.class,
                        item.getTranzactieAsociata().getIdTranzactie());
            });
            layout.add(cmdRecenzie);
        }

        boolean poateRetrage = this.utilizatorCurent != null
                && this.utilizatorCurent.esteVanzator()
                && esteVanzatorulOfertei(item)
                && "in_asteptare".equals(item.getStatus());

        if (poateRetrage) {
            Button cmdRetrage = new Button("Retrage oferta");
            cmdRetrage.addClickListener(e -> retrageOferta(item));
            layout.add(cmdRetrage);
        }

        return layout;
    }

    private void deschideDialogContraOferta(Oferta item, String initiator) {
        if (!poateRaspundeLaNegociere(item)) {
            Notification.show("Nu e rândul tău să negociezi această ofertă!");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Propune un preț nou");

        NumberField pretNouField = new NumberField("Preț propus (lei)");
        pretNouField.setValue(item.getPretNegociat() != null ? item.getPretNegociat() : item.getPret());
        pretNouField.setWidthFull();

        Button cmdTrimite = new Button("Trimite propunerea");
        cmdTrimite.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cmdTrimite.addClickListener(e -> {
            if (pretNouField.getValue() == null || pretNouField.getValue() <= 0) {
                Notification.show("Introdu un preț valid!");
                return;
            }
            trimiteContraOferta(item, pretNouField.getValue(), initiator);
            dialog.close();
        });

        Button cmdRenunta = new Button("Renunță", e -> dialog.close());

        VerticalLayout continut = new VerticalLayout(pretNouField,
                new HorizontalLayout(cmdTrimite, cmdRenunta));
        continut.setPadding(false);
        dialog.add(continut);
        dialog.open();
    }

    private void trimiteContraOferta(Oferta item, Double pretNou, String initiator) {
        if (!poateRaspundeLaNegociere(item)) {
            Notification.show("Nu e rândul tău să negociezi această ofertă!");
            return;
        }

        try {
            this.em.getTransaction().begin();

            Oferta ofertaGestionata = this.em.merge(item);
            ofertaGestionata.proprunNegociere(pretNou, initiator);

            // Notifica cealalta parte despre noua propunere
            Integer idDestinatar = "CUMPARATOR".equals(initiator)
                    ? (ofertaGestionata.getVanzator() != null ? ofertaGestionata.getVanzator().getIdUtilizator() : null)
                    : (ofertaGestionata.getCerere() != null && ofertaGestionata.getCerere().getCumparator() != null
                    ? ofertaGestionata.getCerere().getCumparator().getIdUtilizator() : null);

            if (idDestinatar != null) {
                Utilizator destinatar = this.em.find(Utilizator.class, idDestinatar);
                Notificare notificare = new Notificare(
                        "Ai primit o contra-ofertă de " + pretNou + " lei pentru cererea \""
                                + ofertaGestionata.getCerere().getTitlu() + "\"!",
                        "CONTRA_OFERTA",
                        LocalDateTime.now(),
                        ofertaGestionata.getIdOferta(),
                        "OFERTA"
                );
                destinatar.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();
            Notification.show("Propunere trimisă!");
            reloadOferte();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la trimiterea propunerii: " + ex.getMessage());
        }
    }

    private void acceptaOferta(Oferta item) {
        if (!poateRaspundeLaNegociere(item)) {
            Notification.show("Nu e rândul tău să răspunzi la această ofertă!");
            return;
        }
        try {
            this.em.getTransaction().begin();

            Oferta ofertaGestionata = this.em.merge(item);

            // Daca exista o propunere de negociere activa, pretul final e cel negociat
            if (ofertaGestionata.getPretNegociat() != null) {
                ofertaGestionata.setPret(ofertaGestionata.getPretNegociat());
                ofertaGestionata.anuleazaNegocierea();
            }

            ofertaGestionata.accepta();

            Tranzactie tranzactie = new Tranzactie(
                    LocalDateTime.now(),
                    "finalizata",
                    ofertaGestionata.getPret(),
                    ofertaGestionata
            );
            ofertaGestionata.setTranzactieAsociata(tranzactie);
            this.em.persist(tranzactie);

            this.em.merge(ofertaGestionata.getCerere());

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
        if (!poateRaspundeLaNegociere(item)) {
            Notification.show("Nu e rândul tău să răspunzi la această ofertă!");
            return;
        }
        try {
            this.em.getTransaction().begin();
            Oferta ofertaGestionata = this.em.merge(item);
            ofertaGestionata.anuleazaNegocierea();
            ofertaGestionata.respinge();

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

    private void retrageOferta(Oferta item) {
        if (!esteVanzatorulOfertei(item)) {
            Notification.show("Nu ai dreptul să retragi această ofertă!");
            return;
        }

        try {
            this.em.getTransaction().begin();

            // NOTA: nu folosim Vanzator.retrageOferta() aici, fiindca acea metoda face
            // listaOferte.remove(oferta), iar colectia are orphanRemoval=true - ar sterge
            // definitiv oferta din baza de date in loc sa-i schimbe doar statusul.
            Oferta ofertaGestionata = this.em.merge(item);
            ofertaGestionata.anuleazaNegocierea();
            ofertaGestionata.setStatus("retrasa");

            if (ofertaGestionata.getCerere() != null && ofertaGestionata.getCerere().getCumparator() != null) {
                Utilizator cumparatorEntity = this.em.find(
                        Utilizator.class, ofertaGestionata.getCerere().getCumparator().getIdUtilizator());
                Notificare notificare = new Notificare(
                        "Vânzătorul și-a retras oferta de " + ofertaGestionata.getPret()
                                + " lei pentru cererea \"" + ofertaGestionata.getCerere().getTitlu() + "\".",
                        "OFERTA_RETRASA",
                        LocalDateTime.now(),
                        ofertaGestionata.getIdOferta(),
                        "OFERTA"
                );
                cumparatorEntity.adaugaNotificare(notificare);
                this.em.persist(notificare);
            }

            this.em.getTransaction().commit();
            Notification.show("Ofertă retrasă cu succes!");
            reloadOferte();
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la retragere: " + ex.getMessage());
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