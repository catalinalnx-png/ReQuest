package org.example;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PageTitle("Acasă")
@Route(value = "", layout = MainView.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    private H2 titlu = new H2();
    private Paragraph descriere = new Paragraph();

    public HomeView() {
        setPadding(true);
        addClassNames(LumoUtility.Padding.LARGE);

        titlu.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        descriere.addClassNames(LumoUtility.TextColor.SECONDARY);

        add(titlu, descriere);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UtilizatorSesiune utilizatorCurent = (UtilizatorSesiune)
                VaadinSession.getCurrent().getAttribute(UtilizatorSesiune.class);

        if (utilizatorCurent == null) {
            return; // MainView se ocupă deja de redirect la login
        }

        titlu.setText("Bine ai venit, " + utilizatorCurent.getNume() + "!");
        descriere.setText(utilizatorCurent.esteCumparator()
                ? "Folosește meniul din stânga pentru a-ți vedea cererile sau a adăuga una nouă."
                : "Folosește meniul din stânga pentru a vedea cererile disponibile pe piață.");
    }
}