package eapli.aisafe.app.user.console.presentation;

import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.roles.Roles;
import eapli.framework.actions.ChainedAction;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class FrontMenu extends AbstractUI {
    private static final int EXIT_OPTION = 0;
    private static final int LOGIN_OPTION = 1;
    private static final int LOGIN_WEATHER_OPTION = 2;
    private static final int LOGIN_ATCC_OPTION = 3;
    private static final int LOGIN_PILOT_OPTION = 4;
    private static final int LOGIN_FCO_OPTION = 5;

    @Override
    public boolean show() {
        drawFormTitle();
        return doShow();
    }

    @Override
    public boolean doShow() {
        final var menu = new Menu();
        menu.addItem(LOGIN_OPTION, "Login",
            new ChainedAction(
                new LoginUI(new AuthenticationCredentialHandler(), Roles.ADMIN)::show,
                () -> {
                    new MainMenu().mainLoop();
                    return true;
                }));
        menu.addItem(LOGIN_WEATHER_OPTION, "Login (Weather Person)",
            new ChainedAction(
                new LoginUI(new AuthenticationCredentialHandler(), Roles.WEATHER_PERSON)::show,
                () -> {
                    new MainMenu().mainLoop();
                    return true;
                }));
        menu.addItem(LOGIN_ATCC_OPTION, "Login (Air Transport Company Collaborator)",
            new ChainedAction(
                new LoginUI(new AuthenticationCredentialHandler(), Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR)::show,
                () -> {
                    new MainMenu().mainLoop();
                    return true;
                }));
        menu.addItem(LOGIN_PILOT_OPTION, "Login (Pilot)",
            new ChainedAction(
                new LoginUI(new AuthenticationCredentialHandler(), Roles.PILOT)::show,
                () -> {
                    new MainMenu().mainLoop();
                    return true;
                }));
        menu.addItem(LOGIN_FCO_OPTION, "Login (Flight Control Operator)",
            new ChainedAction(
                new LoginUI(new AuthenticationCredentialHandler(), Roles.FLIGHT_CONTROL_OPERATOR)::show,
                () -> {
                    new MainMenu().mainLoop();
                    return true;
                }));
        menu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "AISafe";
    }
}
