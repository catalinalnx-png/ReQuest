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
public class RegisterView extends VerticalLayout {
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

        Icon iconUser = VaadinIcon.USER_CARD.create();
        iconUser.setSize("48px");
        iconUser.getStyle().set("color", "var(--lumo-primary-color)");

        H1 titlu = new H1("Cont nou");
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

        cmdInregistreaza.addClickListener(e -> inregistreaza());
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
