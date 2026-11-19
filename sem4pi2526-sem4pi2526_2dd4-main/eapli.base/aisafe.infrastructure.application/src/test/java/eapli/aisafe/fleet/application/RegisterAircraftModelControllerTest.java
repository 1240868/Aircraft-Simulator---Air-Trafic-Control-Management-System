package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.ElectricEngineSpecs;
import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.FuelSpecs;
import eapli.aisafe.fleet.domain.FuelType;
import eapli.aisafe.fleet.domain.JetEngineSpecs;
import eapli.aisafe.fleet.domain.Power;
import eapli.aisafe.fleet.domain.Thrust;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class RegisterAircraftModelControllerTest {

    private static final class FakeAuth implements AuthorizationPort {
        Role[] last;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.last = roles;
        }

        @Override
        public String currentUsername() {
            return "bo";
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

    private static final class TestAircraftModelRepo
            extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName, final String manufacturer) {
            return matchOne(e -> e.modelName().equalsIgnoreCase(modelName)
                    && e.manufacturer().equalsIgnoreCase(manufacturer));
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            final AircraftModelId mid = new AircraftModelId(UUID.fromString(id));
            return matchOne(e -> e.identity().equals(mid));
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            return matchOne(e -> e.identity().equals(id));
        }
    }

    private TestEngineRepo engines;
    private TestAircraftModelRepo models;
    private FakeAuth auth;
    private RegisterAircraftModelController controller;

    private static AircraftModel register(final RegisterAircraftModelController ctrl,
                                          final String modelName,
                                          final String manufacturer,
                                          final AircraftType type,
                                          final int numberOfEngines,
                                          final int passengerCapacity,
                                          final double maximumRangeKm,
                                          final List<EngineModelId> certifiedEngineModelIds) {
        return ctrl.register(modelName, manufacturer, type, numberOfEngines, passengerCapacity, maximumRangeKm,
                42_000.0, 78_000.0, 62_000.0, 19_000.0, 12_500.0, 230.0, 122.6, 0.02, 1.5,
                certifiedEngineModelIds);
    }

    private static EngineModel jetEngine(final String name, final String mfr) {
        final var fuel = new FuelSpecs(FuelType.JET_A1, 0.8, 43e6);
        final var specs = new JetEngineSpecs(new Thrust(100, "kN"), new Thrust(30, "kN"), 0.05);
        return EngineModel.createJet(name, mfr, EngineType.TURBOFAN, fuel, specs);
    }

    private static EngineModel electricEngine(final String name, final String mfr) {
        final var fuel = new FuelSpecs(FuelType.ELECTRICITY, 200, 500);
        final var specs = new ElectricEngineSpecs(new Power(1, "MW"), 0.9);
        return EngineModel.createElectric(name, mfr, fuel, specs);
    }

    @Before
    public void setUp() {
        engines = new TestEngineRepo();
        models = new TestAircraftModelRepo();
        auth = new FakeAuth();
        controller = new RegisterAircraftModelController(auth, models, engines);
    }

    @Test
    public void listEngineModelsReturnsRepositoryContents() {
        final int before;
        int n = 0;
        for (final EngineModel ignored : engines.findAll()) {
            n++;
        }
        before = n;
        engines.save(jetEngine("E1-" + UUID.randomUUID().toString().substring(0, 8), "M1"));
        n = 0;
        for (final EngineModel ignored : controller.listEngineModels()) {
            n++;
        }
        assertEquals(before + 1, n);
        assertArrayEquals(new Role[] { Roles.BACKOFFICE_OPERATOR, Roles.ADMIN }, auth.last);
    }

    @Test
    public void registerPersistsWhenEnginesHomogeneous() {
        final EngineModel e1 = engines.save(jetEngine("LEAP-" + UUID.randomUUID().toString().substring(0, 8), "CFMI"));
        final AircraftModel am = register(controller,
                "A320neo-" + UUID.randomUUID().toString().substring(0, 8), "Airbus", AircraftType.PASSENGER, 2, 180, 6000,
                List.of(e1.identity()));

        assertTrue(models.ofIdentity(am.identity()).isPresent());
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenDuplicateModel() {
        final String modelName = "SAME-" + UUID.randomUUID().toString().substring(0, 8);
        final EngineModel e1 = engines.save(jetEngine("T1-" + UUID.randomUUID().toString().substring(0, 8), "M1"));
        register(controller, modelName, "Mfr", AircraftType.PASSENGER, 2, 100, 2000, List.of(e1.identity()));
        register(controller, modelName, "Mfr", AircraftType.PASSENGER, 2, 100, 2000, List.of(e1.identity()));
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenEngineUnknown() {
        register(controller, "X", "Y", AircraftType.PASSENGER, 2, 100, 2000,
                List.of(EngineModelId.newId()));
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenEngineListEmpty() {
        register(controller, "X", "Y", AircraftType.PASSENGER, 2, 100, 2000, List.of());
    }

    @Test
    public void registerWithTwoCertifiedJetEnginesSucceeds() {
        final EngineModel e1 = engines.save(jetEngine("E-A-" + UUID.randomUUID().toString().substring(0, 6), "MFR"));
        final EngineModel e2 = engines.save(jetEngine("E-B-" + UUID.randomUUID().toString().substring(0, 6), "MFR"));
        final AircraftModel am = register(controller,
                "B777-" + UUID.randomUUID().toString().substring(0, 6), "Boeing", AircraftType.PASSENGER, 2, 300, 9000,
                List.of(e1.identity(), e2.identity()));
        assertTrue(models.ofIdentity(am.identity()).isPresent());
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenModelNameDiffersOnlyByCase() {
        final String base = "Neo-" + UUID.randomUUID().toString().substring(0, 6);
        final EngineModel e1 = engines.save(jetEngine("Eng-" + UUID.randomUUID().toString().substring(0, 6), "M1"));
        register(controller, base.toLowerCase(), "Air", AircraftType.PASSENGER, 2, 100, 2000, List.of(e1.identity()));
        register(controller, base.toUpperCase(), "Air", AircraftType.PASSENGER, 2, 100, 2000, List.of(e1.identity()));
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenMotorizationMixedJetAndElectric() {
        final EngineModel jet = engines.save(jetEngine("J1-" + UUID.randomUUID().toString().substring(0, 6), "A"));
        final EngineModel elec = engines.save(electricEngine("E1-" + UUID.randomUUID().toString().substring(0, 6), "B"));
        register(controller, "HYB", "Co", AircraftType.PASSENGER, 2, 50, 1000,
                List.of(jet.identity(), elec.identity()));
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerFailsWhenMotorizationMixedTurbofanAndTurboprop() {
        final var fuel = new FuelSpecs(FuelType.JET_A1, 0.8, 43e6);
        final var specs = new JetEngineSpecs(new Thrust(100, "kN"), new Thrust(30, "kN"), 0.05);
        final EngineModel turbofan = engines.save(
                EngineModel.createJet("TFN", "Co", EngineType.TURBOFAN, fuel, specs));
        final EngineModel turboprop = engines.save(
                EngineModel.createJet("TPR", "Co", EngineType.TURBOPROP, fuel, specs));
        register(controller, "MixWing", "Co", AircraftType.PASSENGER, 2, 50, 1000,
                List.of(turboprop.identity(), turbofan.identity()));
    }
}
