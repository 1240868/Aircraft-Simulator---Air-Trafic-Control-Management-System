package eapli.aisafe.app.user.console.presentation;

import eapli.aisafe.app.user.console.presentation.fleet.AddAircraftUI;
import eapli.aisafe.app.user.console.presentation.pilot.AddPilotUI;
import eapli.aisafe.app.user.console.presentation.pilot.DeactivatePilotUI;
import eapli.aisafe.app.user.console.presentation.pilot.ListPilotRosterUI;
import eapli.aisafe.app.user.console.presentation.fleet.DecommissionAircraftUI;
import eapli.aisafe.app.user.console.presentation.fleet.ListFleetByAgeUI;
import eapli.aisafe.app.user.console.presentation.fleet.ListFleetByMakerUI;
import eapli.aisafe.app.user.console.presentation.fleet.ListFleetByModelUI;
import eapli.aisafe.app.user.console.presentation.fleet.ListFleetByPassengerCapacityUI;
import eapli.aisafe.app.user.console.presentation.flightPlan.InsertWeatherDataUI;
import eapli.aisafe.app.user.console.presentation.flightPlan.RegisterFlightPlanFromFileUI;
import eapli.aisafe.app.user.console.presentation.flightPlan.RegisterFlightPlanUI;
import eapli.aisafe.app.user.console.presentation.flightPlan.TestFlightPlanUI;
import eapli.aisafe.app.user.console.presentation.flightroute.CreateFlightRouteUI;
import eapli.aisafe.app.user.console.presentation.reporting.GenerateMonthlyReportUI;
import eapli.aisafe.app.user.console.presentation.simulation.GenerateSimulationReportUI;
import eapli.aisafe.app.user.console.presentation.weather.ConsultWeatherDataUI;
import eapli.aisafe.app.user.console.presentation.weather.ImportWeatherDataUI;
import eapli.aisafe.app.user.console.presentation.weather.RegisterWeatherDataUI;
import eapli.aisafe.roles.Roles;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractBaseUI {
    private static final int EXIT_OPTION = 0;
    private static final int WEATHER_REGISTER_OPTION = 10;
    private static final int WEATHER_IMPORT_OPTION = 11;
    private static final int WEATHER_CONSULT_OPTION = 12;
    private static final int FLEET_ADD_AIRCRAFT_OPTION = 20;
    private static final int FLEET_DECOMMISSION_OPTION = 21;
    private static final int FLEET_LIST_BY_CAPACITY_OPTION = 22;
    private static final int FLEET_LIST_BY_AGE_OPTION = 23;
    private static final int FLEET_LIST_BY_MODEL_OPTION = 24;
    private static final int FLEET_LIST_BY_MAKER_OPTION = 25;
    private static final int FLIGHT_ROUTE_CREATE_OPTION = 26;
    private static final int PILOT_ADD_OPTION = 27;
    private static final int PILOT_LIST_ROSTER_OPTION = 28;
    private static final int PILOT_DEACTIVATE_OPTION = 29;
    private static final int FLIGHTPLAN_REGISTER_OPTION = 30;
    private static final int FLIGHTPLAN_REGISTER_FROM_FILE_OPTION = 32;
    private static final int FLIGHTPLAN_INSERT_WEATHER_OPTION = 31;
    private static final int FLIGHTPLAN_TEST_OPTION = 33;
    private static final int SIMULATION_REPORT_OPTION = 40;
    private static final int MONTHLY_REPORT_OPTION = 41;

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    @Override
    public boolean doShow() {
        final Menu menu = buildMainMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildMainMenu() {
        final Menu mainMenu = new Menu();

        if (authz.isAuthenticatedUserAuthorizedTo(Roles.WEATHER_PERSON)) {
            mainMenu.addItem(WEATHER_REGISTER_OPTION, "Register weather data",
                () -> new RegisterWeatherDataUI().show());
            mainMenu.addItem(WEATHER_IMPORT_OPTION, "Import bulk weather data",
                () -> new ImportWeatherDataUI().show());
        }

        if (authz.isAuthenticatedUserAuthorizedTo(
                Roles.WEATHER_PERSON, Roles.PILOT, Roles.FLIGHT_CONTROL_OPERATOR)) {
            mainMenu.addItem(WEATHER_CONSULT_OPTION, "Consult weather data",
                () -> new ConsultWeatherDataUI().show());
        }

        if (authz.isAuthenticatedUserAuthorizedTo(Roles.PILOT)) {
            mainMenu.addItem(FLIGHTPLAN_REGISTER_OPTION, "Register flight plan",
                () -> new RegisterFlightPlanUI().show());
            mainMenu.addItem(FLIGHTPLAN_REGISTER_FROM_FILE_OPTION, "Register flight plan from DSL file (US081)",
                () -> new RegisterFlightPlanFromFileUI().show());
            mainMenu.addItem(FLIGHTPLAN_INSERT_WEATHER_OPTION, "Insert weather data in flight plan",
                () -> new InsertWeatherDataUI().show());
            mainMenu.addItem(FLIGHTPLAN_TEST_OPTION, "Test / validate flight plan",
                () -> new TestFlightPlanUI().show());
        }

        if (authz.isAuthenticatedUserAuthorizedTo(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            mainMenu.addItem(FLEET_ADD_AIRCRAFT_OPTION, "Add aircraft to fleet",
                () -> new AddAircraftUI().show());
            mainMenu.addItem(FLEET_DECOMMISSION_OPTION, "Decommission aircraft (retire from fleet)",
                () -> new DecommissionAircraftUI().show());
            mainMenu.addItem(FLEET_LIST_BY_CAPACITY_OPTION, "List fleet by passenger capacity",
                () -> new ListFleetByPassengerCapacityUI().show());
            mainMenu.addItem(FLEET_LIST_BY_AGE_OPTION, "List fleet by aircraft age (years)",
                () -> new ListFleetByAgeUI().show());
            mainMenu.addItem(FLEET_LIST_BY_MODEL_OPTION, "List fleet by aircraft model",
                () -> new ListFleetByModelUI().show());
            mainMenu.addItem(FLEET_LIST_BY_MAKER_OPTION, "List fleet by manufacturer",
                () -> new ListFleetByMakerUI().show());
            mainMenu.addItem(FLIGHT_ROUTE_CREATE_OPTION, "Create flight route",
                () -> new CreateFlightRouteUI().show());
            mainMenu.addItem(PILOT_ADD_OPTION, "Add pilot to company",
                () -> new AddPilotUI().show());
            mainMenu.addItem(PILOT_LIST_ROSTER_OPTION, "List company pilot roster",
                () -> new ListPilotRosterUI().show());
            mainMenu.addItem(PILOT_DEACTIVATE_OPTION, "Remove pilot from roster",
                () -> new DeactivatePilotUI().show());
        }

        if (authz.isAuthenticatedUserAuthorizedTo(Roles.FLIGHT_CONTROL_OPERATOR)) {
            mainMenu.addItem(SIMULATION_REPORT_OPTION, "Generate simulation report (US111)",
                () -> new GenerateSimulationReportUI().show());
            mainMenu.addItem(MONTHLY_REPORT_OPTION, "Generate monthly statistics report (US112)",
                () -> new GenerateMonthlyReportUI().show());
        }

        mainMenu.addItem(MenuItem.separator("--------------"));
        mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
        return mainMenu;
    }
}
