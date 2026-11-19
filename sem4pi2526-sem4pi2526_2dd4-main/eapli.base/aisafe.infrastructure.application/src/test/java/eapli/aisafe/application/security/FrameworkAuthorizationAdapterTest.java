package eapli.aisafe.application.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import eapli.aisafe.roles.Roles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import java.util.Optional;
import org.junit.Test;

public class FrameworkAuthorizationAdapterTest {

    private static SystemUser user(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "Test", "User", username + "@t.pt")
                .withRoles(Roles.ADMIN)
                .build();
    }

    private static final class FakeAuthzService extends AuthorizationService {
        Role[] received;
        final Optional<UserSession> sessionValue;

        FakeAuthzService(final Optional<UserSession> sessionValue) {
            this.sessionValue = sessionValue;
        }

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.received = roles;
        }

        @Override
        public Optional<UserSession> session() {
            return sessionValue;
        }
    }

    @Test
    public void ensureAuthenticatedUserHasAnyOfDelegates() {
        final var delegate = new FakeAuthzService(Optional.empty());
        final AuthorizationPort port = new FrameworkAuthorizationAdapter(delegate);

        port.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);

        assertArrayEquals(new Role[] { Roles.ADMIN, Roles.BACKOFFICE_OPERATOR }, delegate.received);
    }

    @Test
    public void currentUsernameReturnsDelegateUsername() {
        final var su = user("fleet-op");
        final var delegate = new FakeAuthzService(Optional.of(new UserSession(su)));
        final AuthorizationPort port = new FrameworkAuthorizationAdapter(delegate);

        assertEquals("fleet-op", port.currentUsername());
    }

    @Test
    public void currentUsernameFailsWhenNoSession() {
        final var delegate = new FakeAuthzService(Optional.empty());
        final AuthorizationPort port = new FrameworkAuthorizationAdapter(delegate);

        assertThrows(IllegalStateException.class, port::currentUsername);
    }
}
