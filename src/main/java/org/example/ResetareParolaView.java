package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;

@PageTitle("Resetare parolă")
@Route("resetare-parola")
public class ResetareParolaView extends VerticalLayout implements HasUrlParameter<String> {
    private static final long serialVersionUID = 1L;

    private EntityManager em;
    private Utilizator utilizator;
    private String token;

    private H2 titlu = new H2();
    private Paragraph subtitlu = new Paragraph();
    private PasswordField parolaNoua = new PasswordField("Parolă nouă");
    private PasswordField parolaConfirmare = new PasswordField("Confirmă parola nouă");
    private Button cmdReseteaza = new Button("Resetează parola");
    private Anchor linkLogin = new Anchor("login", "Înapoi la autentificare");

    public ResetareParolaView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        initViewLayout();
        cmdReseteaza.addClickListener(e -> reseteazaParola());
    }

    @Override
    public void setParameter(BeforeEvent event, String token) {
        this.token = token;

        try {
            this.utilizator = em.createQuery(
                            "SELECT u FROM Utilizator u WHERE u.resetToken = :token", Utilizator.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException ex) {
            this.utilizator = null;
        }

        boolean tokenValid = this.utilizator != null && this.utilizator.tokenResetareValid(token);

        if (tokenValid) {
            titlu.setText("Setează o parolă nouă");
            subtitlu.setText("Alege o parolă nouă pentru contul tău.");
            parolaNoua.setVisible(true);
            parolaConfirmare.setVisible(true);
            cmdReseteaza.setVisible(true);
        } else {
            titlu.setText("Link invalid sau expirat");
            subtitlu.setText("Acest link de resetare nu mai este valabil. Cere unul nou din pagina de autentificare.");
            parolaNoua.setVisible(false);
            parolaConfirmare.setVisible(false);
            cmdReseteaza.setVisible(false);
        }
    }

    private void initViewLayout() {
        Icon iconKey = VaadinIcon.KEY.create();
        iconKey.setSize("40px");
        iconKey.getStyle().set("color", "var(--lumo-primary-color)");

        titlu.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.NONE);
        subtitlu.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE);

        parolaNoua.setWidthFull();
        parolaConfirmare.setWidthFull();

        cmdReseteaza.setWidthFull();
        cmdReseteaza.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        linkLogin.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout formLayout = new VerticalLayout(
                iconKey, titlu, subtitlu, parolaNoua, parolaConfirmare, cmdReseteaza, linkLogin);
        formLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        Div card = new Div(formLayout);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM,
                LumoUtility.Padding.XLARGE);
        card.setWidth("400px");

        this.add(card);
        this.setSizeFull();
        this.setAlignItems(FlexComponent.Alignment.CENTER);
        this.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        this.addClassNames(LumoUtility.Background.CONTRAST_5);
    }

    private void reseteazaParola() {
        if (this.utilizator == null || !this.utilizator.tokenResetareValid(this.token)) {
            Notification.show("Linkul nu mai este valabil!");
            return;
        }
        if (parolaNoua.getValue().isBlank() || parolaConfirmare.getValue().isBlank()) {
            Notification.show("Completează ambele câmpuri!");
            return;
        }
        if (!parolaNoua.getValue().equals(parolaConfirmare.getValue())) {
            Notification.show("Parolele nu coincid!");
            return;
        }
        if (parolaNoua.getValue().length() < 4) {
            Notification.show("Parola trebuie să aibă minim 4 caractere!");
            return;
        }

        try {
            this.em.getTransaction().begin();
            Utilizator utilizatorGestionat = this.em.merge(this.utilizator);
            utilizatorGestionat.reseteazaParola(parolaNoua.getValue());
            this.em.getTransaction().commit();

            Notification.show("Parola a fost resetată cu succes! Te poți autentifica acum.");
            UI.getCurrent().navigate(LoginView.class);
        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la resetare: " + ex.getMessage());
        }
    }
}
