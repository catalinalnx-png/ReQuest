package org.example;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
public class LoginView extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;

    private TextField email = new TextField("Email");
    private PasswordField parola = new PasswordField("Parolă");
    private Button cmdLogin = new Button("Autentificare");
    private Anchor linkRegister = new Anchor("register", "Nu ai cont? Înregistrează-te aici");

    public LoginView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        this.setSizeFull();
        this.setSpacing(false);
        this.setPadding(false);

        this.add(construiestePanouBranding(), construiestePanouFormular());

        cmdLogin.addClickListener(e -> autentifica());
        parola.addKeyPressListener(Key.ENTER, e -> autentifica());
    }

    private Div construiestePanouBranding() {
        Icon iconCheck1 = VaadinIcon.CHECK_CIRCLE.create();
        Icon iconCheck2 = VaadinIcon.CHECK_CIRCLE.create();
        Icon iconCheck3 = VaadinIcon.CHECK_CIRCLE.create();
        for (Icon icon : new Icon[]{iconCheck1, iconCheck2, iconCheck3}) {
            icon.setColor("white");
            icon.setSize("20px");
        }

        H1 logo = new H1("ReQuest");
        logo.getStyle()
                .set("color", "white")
                .set("font-size", "3rem")
                .set("margin", "0");

        Paragraph tagline = new Paragraph("Piața unde cererile tale întâlnesc oferta potrivită.");
        tagline.getStyle()
                .set("color", "rgba(255,255,255,0.85)")
                .set("font-size", "1.15rem")
                .set("margin-top", "8px")
                .set("max-width", "380px");

        HorizontalLayout feature1 = creazaFeatureLine(iconCheck1, "Publică cereri pentru ce ai nevoie");
        HorizontalLayout feature2 = creazaFeatureLine(iconCheck2, "Primește oferte de la vânzători");
        HorizontalLayout feature3 = creazaFeatureLine(iconCheck3, "Tranzacționează în siguranță");

        VerticalLayout continut = new VerticalLayout(logo, tagline, feature1, feature2, feature3);
        continut.setSpacing(true);
        continut.setPadding(true);
        continut.setAlignItems(FlexComponent.Alignment.START);
        continut.getStyle().set("max-width", "420px");
        continut.addClassNames("glass-card", "fade-in-up");
        continut.getStyle()
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "rgba(255, 255, 255, 0.12)")
                .set("border", "1px solid rgba(255, 255, 255, 0.25)")
                .set("position", "relative")
                .set("z-index", "1");

        Div panou = new Div(continut);
        panou.getStyle()
                .set("background-image",
                        "linear-gradient(135deg, rgba(79,70,229,0.82), rgba(147,51,234,0.82)), "
                                + "url('https://picsum.photos/seed/requestapp-login/900/1200')")
                .set("background-size", "cover")
                .set("background-position", "center")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("height", "100%")
                .set("flex", "1")
                .set("padding", "48px");

        return panou;
    }

    private HorizontalLayout creazaFeatureLine(Icon icon, String text) {
        Span textSpan = new Span(text);
        textSpan.getStyle().set("color", "white").set("font-size", "1rem");

        HorizontalLayout linie = new HorizontalLayout(icon, textSpan);
        linie.setAlignItems(FlexComponent.Alignment.CENTER);
        linie.setSpacing(true);
        linie.getStyle().set("margin-top", "4px");
        return linie;
    }

    private Div construiestePanouFormular() {
        Icon iconLock = VaadinIcon.LOCK.create();
        iconLock.setSize("40px");
        iconLock.getStyle().set("color", "var(--lumo-primary-color)");

        H2 titlu = new H2("Autentificare");
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
        formLayout.setWidth("340px");

        Div panou = new Div(formLayout);
        panou.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("height", "100%")
                .set("flex", "1")
                .set("background", "var(--lumo-base-color)");

        return panou;
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

            String rol;
            if (utilizator instanceof Vanzator) {
                rol = "VANZATOR";
            } else if (utilizator instanceof Admin) {
                rol = "ADMIN";
            } else {
                rol = "CUMPARATOR";
            }

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
