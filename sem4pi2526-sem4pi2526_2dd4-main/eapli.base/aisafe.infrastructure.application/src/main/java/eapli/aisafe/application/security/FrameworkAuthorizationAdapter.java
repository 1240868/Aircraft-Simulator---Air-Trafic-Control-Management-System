package eapli.aisafe.application.security;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Delegates to the EAPLI {@link AuthorizationService} obtained from {@link AuthzRegistry}.
 */
public final class FrameworkAuthorizationAdapter implements AuthorizationPort {

    private final AuthorizationService delegate;

    public FrameworkAuthorizationAdapter(final AuthorizationService delegate) {
        this.delegate = delegate;
    }

    public static AuthorizationPort usingRegistry() {
        return new FrameworkAuthorizationAdapter(AuthzRegistry.authorizationService());
    }

    @Override
    public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
        delegate.ensureAuthenticatedUserHasAnyOf(roles);
    }

    @Override
    public String currentUsername() {
        return delegate.session()
                .orElseThrow(() -> new IllegalStateException("No session"))
                .authenticatedUser()
                .username()
                .toString();
    }
}
