package eapli.aisafe.app.user.console.presentation;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;

public abstract class AbstractBaseUI extends AbstractUI {
    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    @Override
    public String headline() {
        return authz.session()
            .map(s -> "AISafe [ @" + s.authenticatedUser().identity() + " ]")
            .orElse("AISafe [ ==Anonymous== ]");
    }
}
