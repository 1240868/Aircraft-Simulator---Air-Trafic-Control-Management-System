package eapli.aisafe.app.bootstrap;

import eapli.aisafe.app.common.console.BaseApp;
import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;

public final class AisafeBootstrap extends BaseApp {

    private AisafeBootstrap() {
    }

    public static void main(final String[] args) {
        AuthzRegistry.configure(PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(), new PlainTextEncoder());
        new AisafeBootstrap().run(args);
    }

    @Override
    protected void doMain(final String[] args) {
        System.out.println("\n\n------- MASTER DATA -------");
        new AisafeBootstrapper().execute();
    }

    @Override
    protected String appTitle() {
        return "AISafe Bootstrap";
    }

    @Override
    protected String appGoodbye() {
        return "Bootstrap data done.";
    }

    @Override
    protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
        // no event handlers needed for now
    }
}
