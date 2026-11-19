package eapli.aisafe.app.backoffice.console;

import eapli.aisafe.app.backoffice.console.presentation.MainMenu;
import eapli.aisafe.app.common.console.BaseApp;
import eapli.aisafe.app.user.console.presentation.LoginUI;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;

public final class AisafeBackoffice extends BaseApp {

    private AisafeBackoffice() {
    }

    public static void main(final String[] args) {
        AuthzRegistry.configure(PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(), new PlainTextEncoder());
        new AisafeBackoffice().run(args);
    }

    @Override
    protected void doMain(final String[] args) {
        new AisafeBootstrapper().execute();
        if (new LoginUI(new AuthenticationCredentialHandler()).show()) {
            final var menu = new MainMenu();
            menu.mainLoop();
        }
    }

    @Override
    protected String appTitle() {
        return "AISafe Backoffice";
    }

    @Override
    protected String appGoodbye() {
        return "Goodbye from AISafe Backoffice";
    }

    @Override
    protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
    }
}
