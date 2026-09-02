package org.example;

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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@PageTitle("Înregistrare")
@Route("register")
public class RegisterView extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;

    private TextField nume = new TextField("Nume");
    private TextField email = new TextField("Email");
    private PasswordField parola = new PasswordField("Parolă");
    private RadioButtonGroup<String> rol = new RadioButtonGroup<>();
    private Button cmdInregistreaza = new Button("Creează cont");
    private Anchor linkLogin = new Anchor("login", "Ai deja cont? Autentifică-te aici");

    public RegisterView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        this.setSizeFull();
        this.setSpacing(false);
        this.setPadding(false);

        this.add(construiestePanouFormular(), construiestePanouBranding());

        cmdInregistreaza.addClickListener(e -> inregistreaza());
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

        Paragraph tagline = new Paragraph("Alătură-te comunității de cumpărători și vânzători.");
        tagline.getStyle()
                .set("color", "rgba(255,255,255,0.85)")
                .set("font-size", "1.15rem")
                .set("margin-top", "8px")
                .set("max-width", "380px");

        HorizontalLayout feature1 = creazaFeatureLine(iconCheck1, "Cont gratuit, gata în câteva secunde");
        HorizontalLayout feature2 = creazaFeatureLine(iconCheck2, "Alege rolul: cumpărător sau vânzător");
        HorizontalLayout feature3 = creazaFeatureLine(iconCheck3, "Începe imediat să folosești platforma");

        VerticalLayout continut = new VerticalLayout(logo, tagline, feature1, feature2, feature3);
        continut.setSpacing(true);
        continut.setPadding(false);
        continut.setAlignItems(FlexComponent.Alignment.START);
        continut.getStyle().set("max-width", "420px");

        Div panou = new Div(continut);
        panou.getStyle()
                .set("background", "linear-gradient(135deg, #9333ea 0%, #4f46e5 100%)")
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
        Icon iconUser = VaadinIcon.USER_CARD.create();
        iconUser.setSize("40px");
        iconUser.getStyle().set("color", "var(--lumo-primary-color)");

        H2 titlu = new H2("Cont nou");
        titlu.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.NONE);

        Paragraph subtitlu = new Paragraph("Creează-ți un cont ReQuest");
        subtitlu.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE);

        rol.setLabel("Tip cont");
        rol.setItems("Cumpărător", "Vânzător");
        rol.setValue("Cumpărător");
        rol.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        nume.setWidthFull();
        email.setWidthFull();
        parola.setWidthFull();

        cmdInregistreaza.setWidthFull();
        cmdInregistreaza.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        linkLogin.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout formLayout = new VerticalLayout(
                iconUser, titlu, subtitlu, nume, email, parola, rol, cmdInregistreaza, linkLogin);
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

    private void inregistreaza() {
        if (nume.getValue().isBlank() || email.getValue().isBlank() || parola.getValue().isBlank()) {
            Notification.show("Completează toate câmpurile!");
            return;
        }

        try {
            Long count = em.createQuery(
                            "SELECT COUNT(u) FROM Utilizator u WHERE u.email = :email", Long.class)
                    .setParameter("email", email.getValue())
                    .getSingleResult();

            if (count > 0) {
                Notification.show("Există deja un cont cu acest email!");
                return;
            }

            em.getTransaction().begin();

            Utilizator utilizatorNou;
            if ("Vânzător".equals(rol.getValue())) {
                utilizatorNou = new Vanzator(nume.getValue(), email.getValue(), parola.getValue());
            } else {
                utilizatorNou = new Cumparator(nume.getValue(), email.getValue(), parola.getValue());
            }
            em.persist(utilizatorNou);

            em.getTransaction().commit();

            Notification.show("Cont creat cu succes! Te poți autentifica acum.");
            UI.getCurrent().navigate(LoginView.class);

        } catch (Exception ex) {
            if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
            Notification.show("Eroare la înregistrare: " + ex.getMessage());
        }
    }
}
