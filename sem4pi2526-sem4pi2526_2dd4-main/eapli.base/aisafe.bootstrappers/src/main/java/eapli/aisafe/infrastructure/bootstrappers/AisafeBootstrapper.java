package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.usermanagement.domain.AisafeUserBuilderHelper;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.strings.util.Strings;
import eapli.framework.validations.Invariants;
import jakarta.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AisafeBootstrapper implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(AisafeBootstrapper.class);
    private static final String POWERUSER_PWD = "poweruserA1";
    private static final String POWERUSER = "poweruser";

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();
    private final UserRepository userRepository = PersistenceContext.repositories().users();

    @Override
    public boolean execute() {
        final Action[] actions = {
                new MasterUsersBootstrapper(),
                new EngineModelsBootstrapper(),
                new AircraftModelsBootstrapper(),
                new AirTransportCompaniesBootstrapper(),
                new AirControlAreasBootstrapper(),
                new CustomerCollaboratorsBootstrapper(),
                new FleetAircraftBootstrapper()
        };
        registerPowerUser();
        authenticateForBootstrapping();
        var ret = true;
        for (final Action boot : actions) {
            System.out.println("Bootstrapping " + nameOfEntity(boot) + "...");
            ret &= boot.execute();
        }
        return ret;
    }

    private boolean registerPowerUser() {
        if (userRepository.ofIdentity(Username.valueOf(POWERUSER)).isPresent()) {
            LOGGER.warn("Assuming {} already exists", POWERUSER);
            return false;
        }
        final var userBuilder = AisafeUserBuilderHelper.builder();
        userBuilder.withUsername(POWERUSER).withPassword(POWERUSER_PWD)
                .withName("power", "user")
                .withEmail("poweruser@aisafe.com")
                .withRoles(Roles.ADMIN);
        final var newUser = userBuilder.build();
        try {
            userRepository.save(newUser);
            return true;
        } catch (ConcurrencyException | IntegrityViolationException | RollbackException e) {
            LOGGER.warn("Assuming {} already exists", POWERUSER);
            return false;
        }
    }

    protected void authenticateForBootstrapping() {
        authenticationService.authenticate(POWERUSER, POWERUSER_PWD);
        Invariants.ensure(authz.hasSession());
    }

    private String nameOfEntity(final Action boot) {
        final var name = boot.getClass().getSimpleName();
        return Strings.left(name, name.length() - "Bootstrapper".length());
    }
}
