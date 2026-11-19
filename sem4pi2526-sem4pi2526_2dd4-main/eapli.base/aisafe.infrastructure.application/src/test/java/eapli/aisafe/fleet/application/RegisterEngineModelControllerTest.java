package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class RegisterEngineModelControllerTest {

    private static final class FakeAuth implements AuthorizationPort {
        Role[] last;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.last = roles;
        }

        @Override
        public String currentUsername() {
            return "bo-op";
        }
    }

    private static final class TestEngineRepo extends InMemoryDomainRepository<EngineModel, EngineModelId>
            implements EngineModelRepository {

        @Override
        public Optional<EngineModel> findByModelNameAndManufacturer(final String modelName, final String manufacturer) {
            return matchOne(e -> e.modelName().equalsIgnoreCase(modelName)
                    && e.manufacturer().equalsIgnoreCase(manufacturer));
        }
    }

    private TestEngineRepo repo;
    private FakeAuth auth;
    private RegisterEngineModelController controller;

    @Before
    public void setUp() {
        repo = new TestEngineRepo();
        auth = new FakeAuth();
        controller = new RegisterEngineModelController(auth, repo);
    }

    @Test
    public void registerJetPersistsAndChecksRoles() {
        final int before = count(repo);
        final String modelName = "CFM-" + UUID.randomUUID().toString().substring(0, 8);
        final EngineModel em = controller.registerJet(
                modelName, "CFMI", EngineType.TURBOFAN,
                120.0, "kN", 30.0, 0.05, 0.8, 43e6);

        assertNotNull(em.identity());
        assertArrayEquals(new Role[] { Roles.BACKOFFICE_OPERATOR, Roles.ADMIN }, auth.last);
        assertEquals(before + 1, count(repo));
    }

    private static int count(final TestEngineRepo r) {
        int n = 0;
        for (final EngineModel ignored : r.findAll()) {
            n++;
        }
        return n;
    }

    @Test
    public void registerJetAcceptsTurbopropType() {
        final String name = "PW100-" + UUID.randomUUID().toString().substring(0, 8);
        final EngineModel em = controller.registerJet(
                name, "Pratt", EngineType.TURBOPROP,
                50.0, "kN", 20.0, 0.04, 0.8, 43e6);
        assertEquals(EngineType.TURBOPROP, em.type());
    }

    @Test
    public void registerElectricChecksSameRolesAsJet() {
        final String name = "EP-" + UUID.randomUUID().toString().substring(0, 8);
        final EngineModel em = controller.registerElectric(
                name, "EcoJet", 2.0, "MW", 0.92, 250.0, 500.0);

        assertEquals(EngineType.ELECTRIC_PROPELLER, em.type());
        assertArrayEquals(new Role[] { Roles.BACKOFFICE_OPERATOR, Roles.ADMIN }, auth.last);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerJetFailsWhenDuplicate() {
        final String name = "XJ-" + UUID.randomUUID();
        controller.registerJet(name, "M1", EngineType.TURBOJET,
                10.0, "kN", 5.0, 0.1, 0.8, 43e6);
        controller.registerJet(name, "M1", EngineType.TURBOJET,
                11.0, "kN", 6.0, 0.1, 0.8, 43e6);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerJetFailsWhenElectricType() {
        final String name = "Bad-" + UUID.randomUUID().toString().substring(0, 8);
        controller.registerJet(name, "Co", EngineType.ELECTRIC_PROPELLER,
                1.0, "kN", 1.0, 0.1, 0.8, 43e6);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerElectricFailsWhenDuplicate() {
        final String name = "E1-" + UUID.randomUUID();
        controller.registerElectric(name, "Co", 1, "MW", 0.9, 100, 200);
        controller.registerElectric(name, "Co", 2, "MW", 0.9, 100, 200);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerJetFailsWhenDuplicateIgnoringCase() {
        final String name = "Ab-" + UUID.randomUUID().toString().substring(0, 8);
        controller.registerJet(name, "Co", EngineType.TURBOPROP,
                50.0, "kN", 40.0, 0.04, 0.8, 43e6);
        controller.registerJet(name.toLowerCase(), "co", EngineType.TURBOPROP,
                51.0, "kN", 41.0, 0.04, 0.8, 43e6);
    }
}
