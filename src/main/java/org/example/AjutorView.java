package org.example;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PageTitle("Ajutor")
@Route(value = "ajutor", layout = MainView.class)
public class AjutorView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    public AjutorView() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);
        setMaxWidth("880px");
        setSpacing(true);

        add(construiesteHero());

        HorizontalLayout randCarduri = new HorizontalLayout(
                construiesteSectiuneRol(
                        "Cumpărător",
                        new Object[][]{
                                {VaadinIcon.EDIT, "Publici o cerere cu ce cauți, categorie și buget maxim"},
                                {VaadinIcon.INBOX, "Primești oferte de la vânzători interesați"},
                                {VaadinIcon.EXCHANGE, "Accepți, respingi, sau negociezi prin contra-ofertă"},
                                {VaadinIcon.CHECK_CIRCLE, "Tranzacția se creează automat la acceptare"},
                                {VaadinIcon.STAR, "Lași o recenzie vânzătorului după finalizare"}
                        }),
                construiesteSectiuneRol(
                        "Vânzător",
                        new Object[][]{
                                {VaadinIcon.SEARCH, "Explorezi cererile deschise de pe Piață"},
                                {VaadinIcon.PAPERPLANE, "Trimiți o ofertă cu preț și descriere"},
                                {VaadinIcon.EXCHANGE, "Negociezi dacă cumpărătorul propune alt preț"},
                                {VaadinIcon.CLOSE_CIRCLE, "Poți retrage oferta cât timp e în așteptare"},
                                {VaadinIcon.TAGS, "Îți setezi specializările din Profil"}
                        })
        );
        randCarduri.setWidthFull();
        randCarduri.setSpacing(true);
        add(randCarduri);

        add(construiesteFAQ());
    }

    private Div construiesteHero() {
        H1 titlu = new H1("Cum funcționează ReQuest");
        titlu.getStyle().set("color", "white").set("margin", "0").set("font-size", "2rem");

        Paragraph subtitlu = new Paragraph("Ghid rapid pentru cumpărători și vânzători, plus întrebări frecvente.");
        subtitlu.getStyle().set("color", "rgba(255,255,255,0.9)").set("margin-top", "8px");

        VerticalLayout continut = new VerticalLayout(titlu, subtitlu);
        continut.setSpacing(false);
        continut.setPadding(false);

        Div hero = new Div(continut);
        hero.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5 0%, #9333ea 100%)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "32px");
        hero.setWidthFull();
        hero.addClassNames("fade-in-up");
        return hero;
    }

    private Div construiesteSectiuneRol(String titluText, Object[][] pasi) {
        H3 titlu = new H3(titluText);
        titlu.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        VerticalLayout pasiLayout = new VerticalLayout();
        pasiLayout.setSpacing(true);
        pasiLayout.setPadding(false);

        for (Object[] pas : pasi) {
            pasiLayout.add(creazaPas((VaadinIcon) pas[0], (String) pas[1]));
        }

        VerticalLayout continut = new VerticalLayout(titlu, pasiLayout);
        continut.setSpacing(false);
        continut.setPadding(false);

        Div card = new Div(continut);
        card.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE,
                "hover-lift", "fade-in-up");
        card.setWidthFull();
        return card;
    }

    private HorizontalLayout creazaPas(VaadinIcon iconType, String text) {
        Icon icon = iconType.create();
        icon.setSize("18px");
        icon.getStyle().set("color", "white");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5, #9333ea)")
                .set("border-radius", "50%")
                .set("padding", "8px")
                .set("min-width", "34px")
                .set("min-height", "34px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("box-sizing", "border-box");

        Paragraph textParagraph = new Paragraph(text);
        textParagraph.addClassNames(LumoUtility.Margin.NONE);

        HorizontalLayout linie = new HorizontalLayout(iconWrapper, textParagraph);
        linie.setAlignItems(FlexComponent.Alignment.CENTER);
        linie.setSpacing(true);
        return linie;
    }

    private Div construiesteFAQ() {
        Icon icon = VaadinIcon.CHAT.create();
        icon.setSize("18px");
        icon.getStyle().set("color", "white");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5, #9333ea)")
                .set("border-radius", "50%")
                .set("padding", "10px")
                .set("display", "inline-flex");

        H3 titlu = new H3("Întrebări frecvente");
        titlu.addClassNames(LumoUtility.Margin.NONE);

        HorizontalLayout titluLayout = new HorizontalLayout(iconWrapper, titlu);
        titluLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titluLayout.setSpacing(true);
        titluLayout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        Accordion accordion = new Accordion();
        accordion.setWidthFull();

        String[][] faq = {
                {"Ce înseamnă statusurile unei cereri?",
                        "„deschisă”/„activă” - încă poate primi oferte. „închisă” - o ofertă a fost acceptată. "
                                + "„anulată” - cumpărătorul a renunțat la cerere."},
                {"Cum funcționează negocierea?",
                        "Oricare parte poate propune un preț nou printr-o contra-ofertă. E rândul celeilalte "
                                + "părți să răspundă: acceptă, respinge, sau propune la rândul ei alt preț."},
                {"Cum îmi resetez parola?",
                        "Din meniul „Setări cont” poți schimba parola oricând, dacă știi parola actuală."},
                {"Ce se întâmplă dacă o cerere expiră?",
                        "Cererile cu data limită depășită sunt marcate „Expirată” și nu mai pot primi oferte noi."}
        };

        for (String[] intrebare : faq) {
            AccordionPanel panel = accordion.add(intrebare[0], new Paragraph(intrebare[1]));
            panel.addThemeVariants();
        }

        VerticalLayout continut = new VerticalLayout(titluLayout, accordion);
        continut.setSpacing(false);
        continut.setPadding(false);

        Div card = new Div(continut);
        card.addClassNames(
                LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, "fade-in-up");
        card.setWidthFull();
        return card;
    }
}