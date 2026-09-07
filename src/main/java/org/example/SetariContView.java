package org.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@PageTitle("Setări cont")
@Route(value = "setari-cont", layout = MainView.class)
public class SetariContView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private UtilizatorSesiune utilizatorCurent;
    private Utilizator utilizator;

    private H2 titlu = new H2("Setări cont");
    private PasswordField parolaVeche = new PasswordField("Parolă actuală");
    private PasswordField parolaNoua = new PasswordField("Parolă nouă");
    private PasswordField parolaNouaConfirmare = new PasswordField("Confirmă parola nouă");
    private Button cmdSchimba = new Button("Schimbă parola");

    public SetariContView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        initControllerActions();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (this.utilizatorCurent == null) {
            return; // MainView se ocupă deja de redirect la login
        }

        this.utilizator = em.find(Utilizator.class, this.utilizatorCurent.getIdUtilizator());
    }

    private void initViewLayout() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);
        setMaxWidth("500px");

        Paragraph explicatie = new Paragraph("Schimbă parola contului tău. Ai nevoie de parola actuală pentru confirmare.");
        explicatie.addClassNames(LumoUtility.TextColor.SECONDARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(parolaVeche, parolaNoua, parolaNouaConfirmare);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        cmdSchimba.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div card = new Div(explicatie, formLayout, cmdSchimba);
        card.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE);
        card.setWidthFull();

        this.add(titlu, card);
    }

    private void initControllerActions() {
        cmdSchimba.addClickListener(e -> schimbaParola());
    }

    private void schimbaParola() {
        if (this.utilizator == null) {
            Notification.show("Eroare: contul nu a putut fi identificat!");
            return;
        }
        if (parolaVeche.getValue().isBlank() || parolaNoua.getValue().isBlank()
                || parolaNouaConfirmare.getValue().isBlank()) {
            Notification.show("Completează toate câmpurile!");
            return;
        }
        if (!this.utilizator.autentificare(this.utilizator.getEmail(), parolaVeche.getValue())) {
            Notification.show("Parola actuală este incorectă!");
            return;
        }
        if (!parolaNoua.getValue().equals(parolaNouaConfirmare.getValue())) {
            Notification.show("Parola nouă și confirmarea nu coincid!");
            return;
        }
        if (parolaNoua.getValue().length() < 4) {
            Notification.show("Parola nouă trebuie să aibă minim 4 caractere!");
            return;
        }

        try {
            this.em.getTransaction().begin();
            Utilizator utilizatorGestionat = this.em.merge(this.utilizator);
            utilizatorGestionat.setParolaHash(parolaNoua.getValue());
            this.em.getTransaction().commit();

            this.utilizator = utilizatorGestionat;
            parolaVeche.clear();
            parolaNoua.clear();
            parolaNouaConfirmare.clear();
            Notification.show("Parola a fost schimbată cu succes!");
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la schimbarea parolei: " + ex.getMessage());
        }
    }
}
