package eapli.aisafe.app.user.console;

import eapli.aisafe.app.user.console.presentation.FrontMenu;
import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;

public final class AisafeUserApp {
    private AisafeUserApp() {
    }

    public static void main(final String[] args) {
        System.out.println("=====================================");
        System.out.println("AISafe User App");
        System.out.println("(C) 2026, AISafe Team 4");
        System.out.println("=====================================");

        AuthzRegistry.configure(PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(), new PlainTextEncoder());

        new AisafeBootstrapper().execute();
        final AuthorizationService authz = AuthzRegistry.authorizationService();
        authz.clearSession();

        new FrontMenu().show();

        System.exit(0);
    }
}
