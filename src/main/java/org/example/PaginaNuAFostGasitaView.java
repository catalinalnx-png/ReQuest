package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.theme.lumo.LumoUtility;

// Vaadin detecteaza automat aceasta clasa la pornire (fara @Route) si o foloseste
// pentru orice ruta care nu exista - inlocuieste pagina alba/urata implicita.
public class PaginaNuAFostGasitaView extends VerticalLayout implements HasErrorParameter<NotFoundException> {
    private static final long serialVersionUID = 1L;

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        Icon icon = VaadinIcon.MAP_MARKER.create();
        icon.setSize("48px");
        icon.getStyle().set("color", "white");

        Div iconWrapper = new Div(icon);
        iconWrapper.getStyle()
                .set("background", "linear-gradient(135deg, #4f46e5 0%, #9333ea 100%)")
                .set("border-radius", "50%")
                .set("padding", "24px")
                .set("display", "inline-flex");

        H1 titlu = new H1("404");
        titlu.getStyle().set("margin", "16px 0 0 0").set("font-size", "3rem");

        Paragraph subtitlu = new Paragraph("Pagina pe care o cauți nu există sau a fost mutată.");
        subtitlu.addClassNames(LumoUtility.TextColor.SECONDARY);

        Button cmdAcasa = new Button("Înapoi la Acasă", VaadinIcon.HOME.create());
        cmdAcasa.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cmdAcasa.addClickListener(e -> UI.getCurrent().navigate(""));

        VerticalLayout continut = new VerticalLayout(iconWrapper, titlu, subtitlu, cmdAcasa);
        continut.setAlignItems(FlexComponent.Alignment.CENTER);
        continut.setSpacing(true);
        continut.setPadding(false);

        Div card = new Div(continut);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM,
                LumoUtility.Padding.XLARGE);
        card.setWidth("420px");

        this.add(card);
        this.setSizeFull();
        this.setAlignItems(FlexComponent.Alignment.CENTER);
        this.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        this.addClassNames(LumoUtility.Background.CONTRAST_5);

        return 404;
    }
}