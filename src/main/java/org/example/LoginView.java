package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;

@PageTitle("Autentificare")
@Route("login")
public class LoginView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;

    private TextField email = new TextField("Email");
    private PasswordField parola = new PasswordField("Parolă");
    private Button cmdLogin = new Button("Autentificare");
    private Anchor linkRegister = new Anchor("register", "Nu ai cont? Înregistrează-te aici");

    public LoginView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        Icon iconLock = VaadinIcon.LOCK.create();
        iconLock.setSize("48px");
        iconLock.getStyle().set("color", "var(--lumo-primary-color)");

        H1 titlu = new H1("Autentificare");
        titlu.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.NONE);

        Paragraph subtitlu = new Paragraph("Conectează-te la contul tău ReQuest");
        subtitlu.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE);

        email.setWidthFull();
        parola.setWidthFull();

        cmdLogin.setWidthFull();
        cmdLogin.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        linkRegister.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout formLayout = new VerticalLayout(
                iconLock, titlu, subtitlu, email, parola, cmdLogin, linkRegister);
        formLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        Div card = new Div(formLayout);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM,
                LumoUtility.Padding.XLARGE);
        card.setWidth("380px");

        this.add(card);
        this.setSizeFull();
        this.setAlignItems(FlexComponent.Alignment.CENTER);
        this.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        this.addClassNames(LumoUtility.Background.CONTRAST_5);

        cmdLogin.addClickListener(e -> autentifica());
        parola.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> autentifica());
    }

    private void autentifica() {
        String emailIntrodus = email.getValue();
        String parolaIntrodusa = parola.getValue();

        if (emailIntrodus == null || emailIntrodus.isBlank()
                || parolaIntrodusa == null || parolaIntrodusa.isBlank()) {
            Notification.show("Completează email și parolă!");
            return;
        }

        try {
            Utilizator utilizator = em.createQuery(
                            "SELECT u FROM Utilizator u WHERE u.email = :email", Utilizator.class)
                    .setParameter("email", emailIntrodus)
                    .getSingleResult();

            if (!utilizator.autentificare(emailIntrodus, parolaIntrodusa)) {
                Notification.show("Parolă incorectă!");
                return;
            }

            String rol = (utilizator instanceof Vanzator) ? "VANZATOR" : "CUMPARATOR";

            UtilizatorSesiune sesiune = new UtilizatorSesiune(
                    utilizator.getIdUtilizator(),
                    utilizator.getNume(),
                    utilizator.getEmail(),
                    rol
            );

            VaadinSession.getCurrent().setAttribute(UtilizatorSesiune.class, sesiune);

            Notification.show("Bun venit, " + utilizator.getNume() + "!");
            UI.getCurrent().navigate(HomeView.class);

        } catch (NoResultException ex) {
            Notification.show("Nu există niciun utilizator cu acest email!");
        } catch (Exception ex) {
            Notification.show("Eroare la autentificare: " + ex.getMessage());
        }
    }
}