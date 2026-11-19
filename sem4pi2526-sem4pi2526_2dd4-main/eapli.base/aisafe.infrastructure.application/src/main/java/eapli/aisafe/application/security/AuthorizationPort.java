package eapli.aisafe.application.security;

import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Narrow port for use-case controllers so they can be unit-tested without static {@code AuthzRegistry}.
 */
public interface AuthorizationPort {

    void ensureAuthenticatedUserHasAnyOf(Role... roles);

    /**
     * @throws IllegalStateException if there is no authenticated session
     */
    String currentUsername();
}
