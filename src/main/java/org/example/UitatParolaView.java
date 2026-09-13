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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;

@PageTitle("Am uitat parola")
@Route("uitat-parola")
public class UitatParolaView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private EntityManager em;

    private TextField email = new TextField("Email");
    private Button cmdTrimite = new Button("Trimite link de resetare");
    private Div rezultatContainer = new Div();
    private Anchor linkLogin = new Anchor("login", "Înapoi la autentificare");

    public UitatParolaView() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("REQUESTJPA");
        this.em = emf.createEntityManager();

        Icon iconMail = VaadinIcon.ENVELOPE_O.create();
        iconMail.setSize("40px");
        iconMail.getStyle().set("color", "var(--lumo-primary-color)");

        H2 titlu = new H2("Am uitat parola");
        titlu.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.NONE);

        Paragraph subtitlu = new Paragraph("Introdu emailul contului tău și îți generăm un link de resetare.");
        subtitlu.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE);

        email.setWidthFull();
        cmdTrimite.setWidthFull();
        cmdTrimite.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        rezultatContainer.setWidthFull();
        rezultatContainer.setVisible(false);

        linkLogin.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout formLayout = new VerticalLayout(
                iconMail, titlu, subtitlu, email, cmdTrimite, rezultatContainer, linkLogin);
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

        cmdTrimite.addClickListener(e -> genereazaLinkResetare());
    }

    private void genereazaLinkResetare() {
        String emailIntrodus = email.getValue();
        if (emailIntrodus == null || emailIntrodus.isBlank()) {
            Notification.show("Introdu un email!");
            return;
        }

        try {
            Utilizator utilizator = em.createQuery(
                            "SELECT u FROM Utilizator u WHERE u.email = :email", Utilizator.class)
                    .setParameter("email", emailIntrodus)
                    .getSingleResult();

            String token;
            try {
                this.em.getTransaction().begin();
                Utilizator utilizatorGestionat = this.em.merge(utilizator);
                token = utilizatorGestionat.genereazaTokenResetare();
                this.em.getTransaction().commit();
            } catch (Exception ex) {
                if (this.em.getTransaction().isActive()) this.em.getTransaction().rollback();
                throw ex;
            }

            afiseazaLinkSimulat(token);

        } catch (NoResultException ex) {
            // NOTA: din motive de securitate (evitarea "user enumeration"), o aplicatie reala
            // ar arata mereu acelasi mesaj generic, indiferent daca emailul exista sau nu.
            // Aici afisam un mesaj diferit doar ca sa fie clar la testare ce se intampla.
            Notification.show("Nu există niciun cont cu acest email!");
        } catch (Exception ex) {
            Notification.show("Eroare: " + ex.getMessage());
        }
    }

    private void afiseazaLinkSimulat(String token) {
        rezultatContainer.removeAll();
        rezultatContainer.setVisible(true);

        Paragraph explicatie = new Paragraph(
                "Aplicația nu are un server de email configurat. Într-o aplicație reală, acest link "
                        + "ar fi trimis pe adresa ta de email. Pentru testare, îl afișăm direct aici:");
        explicatie.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Anchor linkResetare = new Anchor("resetare-parola/" + token, "Resetează-ți parola acum");
        linkResetare.getElement().setAttribute("router-ignore", true);
        linkResetare.getStyle()
                .set("display", "inline-block")
                .set("padding", "8px 16px")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-weight", "600")
                .set("word-break", "break-all");

        Paragraph valabilitate = new Paragraph("Linkul e valabil 30 de minute.");
        valabilitate.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);

        rezultatContainer.add(explicatie, linkResetare, valabilitate);
        rezultatContainer.addClassNames(LumoUtility.Margin.Top.SMALL);
    }
}
