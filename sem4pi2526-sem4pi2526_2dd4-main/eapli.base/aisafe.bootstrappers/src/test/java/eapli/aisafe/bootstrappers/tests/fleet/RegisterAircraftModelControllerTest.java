package eapli.aisafe.bootstrappers.tests.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.fleet.application.RegisterAircraftModelController;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.FuelSpecs;
import eapli.aisafe.fleet.domain.FuelType;
import eapli.aisafe.fleet.domain.JetEngineSpecs;
import eapli.aisafe.fleet.domain.Thrust;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.AisafeUserBuilderHelper;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegisterAircraftModelControllerTest {

    private static final String OP_USER = "ac-model-test-op";
    private static final String OP_PWD = "secret1";

    @BeforeClass
    public static void installAuthAndOperator() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(),
                new PlainTextEncoder());
        final var users = PersistenceContext.repositories().users();
        try {
            final var u = AisafeUserBuilderHelper.builder()
                    .withUsername(OP_USER)
                    .withPassword(OP_PWD)
                    .withName("Fleet", "Tester")
                    .withEmail("fleet-tester@aisafe.com")
                    .withRoles(Roles.BACKOFFICE_OPERATOR)
                    .build();
            users.save(u);
        } catch (final Exception ignored) {
            // user may already exist when tests are re-run in the same JVM
        }
    }

    @Before
    public void authenticate() {
        AuthzRegistry.authenticationService().authenticate(OP_USER, OP_PWD);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

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

    private static EngineModel saveTurbofanEngine(final String modelName, final String manufacturer) {
        final var fuel = new FuelSpecs(FuelType.JET_A1, 0.8, 43e6);
        final var specs = new JetEngineSpecs(
                new Thrust(120, "kN"),
                new Thrust(80, "kN"),
                0.04);
        final var em = EngineModel.createJet(modelName, manufacturer, EngineType.TURBOFAN, fuel, specs);
        return PersistenceContext.repositories().engineModels().save(em);
    }

    private static EngineModel saveTurbopropEngine(final String modelName, final String manufacturer) {
        final var fuel = new FuelSpecs(FuelType.JET_A1, 0.75, 43e6);
        final var specs = new JetEngineSpecs(
                new Thrust(50, "kN"),
                new Thrust(40, "kN"),
                0.05);
        final var em = EngineModel.createJet(modelName, manufacturer, EngineType.TURBOPROP, fuel, specs);
        return PersistenceContext.repositories().engineModels().save(em);
    }

    @Test
    public void listEngineModelsShouldReturnSeededEngines() {
        saveTurbofanEngine("ListSeed-" + suffix(), "ACME");
        final var ctrl = new RegisterAircraftModelController();
        boolean found = false;
        for (final var em : ctrl.listEngineModels()) {
            if (em.modelName().startsWith("ListSeed-")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void registerShouldPersistWhenEnginesAreConsistent() {
        final EngineModel e1 = saveTurbofanEngine("RegA-" + suffix(), "ACME");
        final EngineModel e2 = saveTurbofanEngine("RegB-" + suffix(), "ACME");
        final var ctrl = new RegisterAircraftModelController();
        final String modelName = "Widebody-" + suffix();
        final var saved = register(ctrl,
                modelName,
                "ACME",
                AircraftType.PASSENGER,
                2,
                220,
                9000.0,
                List.of(e1.identity(), e2.identity()));
        assertNotNull(saved.identity());
        assertEquals(modelName, saved.modelName());
        assertEquals(2, saved.certifiedEngineModelIds().size());
    }

    @Test
    public void registerShouldRejectDuplicateModelNameAndManufacturer() {
        final EngineModel e = saveTurbofanEngine("DupEng-" + suffix(), "MFR");
        final var ctrl = new RegisterAircraftModelController();
        final String modelName = "DupModel-" + suffix();
        register(ctrl, modelName, "MFR", AircraftType.CARGO, 1, 2, 1000.0, List.of(e.identity()));
        assertThrows(IntegrityViolationException.class, () -> register(
                ctrl,
                modelName,
                "MFR",
                AircraftType.CARGO,
                1,
                2,
                2000.0,
                List.of(e.identity())));
    }

    @Test
    public void registerShouldRejectUnknownEngineId() {
        final var ctrl = new RegisterAircraftModelController();
        assertThrows(IntegrityViolationException.class, () -> register(
                ctrl,
                "X-" + suffix(),
                "MFR",
                AircraftType.MIXED,
                1,
                10,
                500.0,
                List.of(EngineModelId.newId())));
    }

    @Test
    public void registerShouldRejectEmptyCertifiedEngines() {
        final var ctrl = new RegisterAircraftModelController();
        assertThrows(IntegrityViolationException.class, () -> register(
                ctrl,
                "Empty-" + suffix(),
                "MFR",
                AircraftType.PASSENGER,
                2,
                50,
                3000.0,
                List.of()));
    }

    @Test
    public void registerShouldRejectMixedEngineMotorization() {
        final EngineModel jet = saveTurbofanEngine("MixJet-" + suffix(), "MFR");
        final EngineModel tp = saveTurbopropEngine("MixTp-" + suffix(), "MFR");
        final var ctrl = new RegisterAircraftModelController();
        assertThrows(IntegrityViolationException.class, () -> register(
                ctrl,
                "Mixed-" + suffix(),
                "MFR",
                AircraftType.MIXED,
                2,
                20,
                1500.0,
                List.of(jet.identity(), tp.identity())));
    }

    @Test
    public void registerShouldAcceptSingleCertifiedEngine() {
        final EngineModel e = saveTurbofanEngine("Single-" + suffix(), "MFR");
        final var ctrl = new RegisterAircraftModelController();
        final var saved = register(ctrl,
                "SingleAc-" + suffix(),
                "MFR",
                AircraftType.CARGO,
                1,
                2,
                800.0,
                List.of(e.identity()));
        assertEquals(1, saved.certifiedEngineModelIds().size());
    }

    @Test
    public void registerShouldIterateMutableIdListFromCaller() {
        final EngineModel e = saveTurbofanEngine("MutEng-" + suffix(), "MFR");
        final var ids = new ArrayList<EngineModelId>();
        ids.add(e.identity());
        final var ctrl = new RegisterAircraftModelController();
        assertNotNull(register(ctrl, "MutAc-" + suffix(), "MFR", AircraftType.PASSENGER, 1, 30, 1200.0, ids));
    }
}
