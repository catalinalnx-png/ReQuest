package org.example;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * This class is used to configure the generated html host page used by the app
 */
@PWA(name = "My Application", shortName = "My Application")
@Theme(value = "requestapp")
public class AppShell implements AppShellConfigurator {

}
