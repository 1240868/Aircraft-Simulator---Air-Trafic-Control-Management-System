package eapli.aisafe.app.backoffice.console.presentation;

import eapli.aisafe.app.backoffice.console.presentation.aircontrol.RegisterAirControlAreaUI;
import eapli.aisafe.app.backoffice.console.presentation.airport.RegisterAirportUI;
import eapli.aisafe.app.backoffice.console.presentation.authz.AddUserUI;
import eapli.aisafe.app.backoffice.console.presentation.company.AddCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.company.ListCollaboratorsUI;
import eapli.aisafe.app.backoffice.console.presentation.company.RegisterAirTransportCompanyUI;
import eapli.aisafe.app.backoffice.console.presentation.fleet.RegisterAircraftModelUI;
import eapli.aisafe.app.backoffice.console.presentation.fleet.RegisterEngineModelUI;
import eapli.aisafe.roles.Roles;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {
    private static final int EXIT_OPTION = 0;
    private static final int USERS_OPTION = 1;
    private static final int AIR_CONTROL_OPTION = 2;
    private static final int AIRPORT_OPTION = 3;
    private static final int CUSTOMER_COLLABORATORS_OPTION = 4;
    private static final int FLEET_OPTION = 5;
    private static final int ADD_USER_OPTION = 1;
    private static final int REGISTER_AREA_OPTION = 1;
    private static final int REGISTER_AIRPORT_OPTION = 1;
    private static final int REGISTER_COMPANY_OPTION = 1;
    private static final int REGISTER_COLLABORATOR_OPTION = 2;
    private static final int LIST_COLLABORATORS_OPTION = 3;
    private static final int REGISTER_ENGINE_MODEL_OPTION = 1;
    private static final int REGISTER_AIRCRAFT_MODEL_OPTION = 2;
    private static final String RETURN_LABEL = "Return";
    private static final String SEPARATOR_LABEL = "--------------";

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    @Override
    public boolean show() {
        drawFormTitle();
        return doShow();
    }

    @Override
    public boolean doShow() {
        final Menu menu = buildMainMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return authz.session()
            .map(s -> "Backoffice [ @" + s.authenticatedUser().identity() + " ]")
            .orElse("Backoffice [ ==Anonymous== ]");
    }

    private Menu buildMainMenu() {
        final var mainMenu = new Menu();

        if (authz.isAuthenticatedUserAuthorizedTo(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR)) {
            mainMenu.addSubMenu(USERS_OPTION, buildUsersMenu());
            mainMenu.addSubMenu(AIR_CONTROL_OPTION, buildAirControlMenu());
            mainMenu.addSubMenu(AIRPORT_OPTION, buildAirportMenu());
            mainMenu.addSubMenu(CUSTOMER_COLLABORATORS_OPTION, buildCustomerCollaboratorsMenu());
            mainMenu.addSubMenu(FLEET_OPTION, buildFleetMenu());
        }

        mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
        mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
        return mainMenu;
    }

    private Menu buildUsersMenu() {
        final var menu = new Menu("Users >");
        menu.addItem(ADD_USER_OPTION, "Add User", new AddUserUI()::show);
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }

    private Menu buildAirControlMenu() {
        final var menu = new Menu("Air Control >");
        menu.addItem(REGISTER_AREA_OPTION, "Register Air Control Area", new RegisterAirControlAreaUI()::show);
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }

    private Menu buildAirportMenu() {
        final var menu = new Menu("Airports >");
        menu.addItem(REGISTER_AIRPORT_OPTION, "Register Airport", new RegisterAirportUI()::show);
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }

    private Menu buildCustomerCollaboratorsMenu() {
        final var menu = new Menu("Customer collaborators >");
        menu.addItem(REGISTER_COMPANY_OPTION, "Register air transport company",
                new RegisterAirTransportCompanyUI()::show);
        menu.addItem(REGISTER_COLLABORATOR_OPTION, "Register customer collaborator",
                new AddCollaboratorUI()::show);
        menu.addItem(LIST_COLLABORATORS_OPTION, "List customer collaborators (active only)",
                new ListCollaboratorsUI()::show);
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }

    private Menu buildFleetMenu() {
        final var menu = new Menu("Fleet >");
        menu.addItem(REGISTER_ENGINE_MODEL_OPTION, "Register engine model",
                new RegisterEngineModelUI()::show);
        menu.addItem(REGISTER_AIRCRAFT_MODEL_OPTION, "Register aircraft model",
                new RegisterAircraftModelUI()::show);
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }
}
