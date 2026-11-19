package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import jakarta.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

public class AbstractUserBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserBootstrapper.class);

    final AddUserController userController = new AddUserController();
    private final UserRepository users = PersistenceContext.repositories().users();

    protected SystemUser registerUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles) {
        final var existing = users.ofIdentity(Username.valueOf(username));
        if (existing.isPresent()) {
            LOGGER.warn("Assuming {} already exists", username);
            return existing.get();
        }
        try {
            final SystemUser created = userController.addUser(username, password, firstName, lastName, email, roles);
            LOGGER.debug("Registered user {}", username);
            return created;
        } catch (final IntegrityViolationException | ConcurrencyException | RollbackException e) {
            LOGGER.warn("Assuming {} already exists", username);
            return users.ofIdentity(Username.valueOf(username)).orElse(null);
        }
    }
}
