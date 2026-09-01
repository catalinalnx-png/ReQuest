package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;


@Route
public class MainView extends VerticalLayout implements RouterLayout {
    public MainView() {
        setMenuBar();
        Button button = new Button("Click me",
                event -> add(new Paragraph("Clicked!")));
        add(button);
    }

    private void setMenuBar() {
        MenuBar mainMenu = new MenuBar();

        MenuItem homeMenu = mainMenu.addItem("Home");
        homeMenu.addClickListener(event -> UI.getCurrent().navigate(MainView.class));

        MenuItem gridFormsCereriMenu = mainMenu.addItem("Cereri");
        SubMenu gridFormsCereriSubMenu = gridFormsCereriMenu.getSubMenu();
        gridFormsCereriSubMenu.addItem("Lista Cereri...",
                event -> UI.getCurrent().navigate("lista-cereri")); // Asigură-te că ruta există
        gridFormsCereriSubMenu.addItem("Form Editare Cerere...",
                event -> UI.getCurrent().navigate("cerere-form"));
//
        add(new HorizontalLayout(mainMenu));
    }
}