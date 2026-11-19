package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.FakeAuth;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestAircraftRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestModelRepo;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftOperationalStatus;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.roles.Roles;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

/** US072c — list company's active fleet filtered by passenger capacity. */
public class ListFleetByPassengerCapacityControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ListFleetByPassengerCapacityController());
    }

    private static final LocalDate MFG = LocalDate.of(2014, 2, 2);

    private static AircraftModel sampleModel(final String name, final String maker) {
        return new AircraftModel(
                name, maker, AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
    }

    @Test
    public void listShouldReturnOnlyAircraftMatchingExactCapacitySortedByRegistration() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel modelA = sampleModel("A320", "Airbus");
        final AircraftModelId midA = modelA.identity();
        models.save(modelA);
        final AircraftModel modelB = sampleModel("E195", "Embraer");
        models.save(modelB);

        aircraft.activeByCapacity.add(AircraftFactory.registerNew("CS-BBB", "PT", modelB.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null));
        aircraft.activeByCapacity.add(AircraftFactory.registerNew("CS-AAA", "PT", midA, cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null));

        final var ctrl = new ListFleetByPassengerCapacityController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByPassengerCapacity("100");

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(2, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
        assertEquals("CS-AAA", r.aircraft().get(0).registrationNumber());
        assertEquals("CS-BBB", r.aircraft().get(1).registrationNumber());
    }

    @Test
    public void listShouldIncludeMaintenanceButExcludeRetired() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel("A320", "Airbus");
        models.save(model);
        aircraft.activeByCapacity.add(AircraftFactory.registerWithOperationalStatus(
                "CS-MNT", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 40)), 180, MFG, null,
                AircraftOperationalStatus.MAINTENANCE));

        final var ctrl = new ListFleetByPassengerCapacityController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByPassengerCapacity("40");

        assertEquals(1, r.aircraft().size());
        assertEquals("MAINTENANCE", r.aircraft().get(0).operationalStatus());
    }

    @Test
    public void listShouldReturnEmptyWithMessageWhenNoMatches() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        models.save(sampleModel("A320", "Airbus"));
        final var ctrl = new ListFleetByPassengerCapacityController(auth, new TestAircraftRepo(), models, companies);

        final FleetListResult r = ctrl.listFleetByPassengerCapacity("99");

        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_CAPACITY, r.messageWhenEmpty());
    }

    @Test
    public void listShouldFailWhenUserNotLinkedToCompany() {
        final var ctrl = new ListFleetByPassengerCapacityController(new FakeAuth(), new TestAircraftRepo(),
                new TestModelRepo(), new TestCompanyRepo());

        assertThrows(IllegalStateException.class, () -> ctrl.listFleetByPassengerCapacity("50"));
    }

    @Test
    public void listShouldFailWhenCapacityNull() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByPassengerCapacityController(auth, new TestAircraftRepo(),
                new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity(null));
    }

    @Test
    public void listShouldFailWhenCapacityMissingOrBlank() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByPassengerCapacityController(auth, new TestAircraftRepo(),
                new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity(null));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity(""));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity("   "));
    }

    @Test
    public void listShouldFailWhenCapacityNotPositive() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByPassengerCapacityController(auth, new TestAircraftRepo(),
                new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity("0"));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity("-5"));
    }

    @Test
    public void listShouldFailWhenCapacityNotAnInteger() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByPassengerCapacityController(auth, new TestAircraftRepo(),
                new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity("12.5"));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByPassengerCapacity("abc"));
    }

    @Test
    public void listShouldAcceptCapacityWithWhitespace() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel("A320", "Airbus");
        models.save(model);
        aircraft.activeByCapacity.add(AircraftFactory.registerNew("CS-ZZZ", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 55)), 180, MFG, null));

        final var ctrl = new ListFleetByPassengerCapacityController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByPassengerCapacity("  55  ");

        assertEquals(1, r.aircraft().size());
    }

    @Test
    public void listShouldFailWhenModelMissingForAircraft() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel("A320", "Airbus");
        final Aircraft a = AircraftFactory.registerNew("CS-ORP", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 33)), 180, MFG, null);
        aircraft.activeByCapacity.add(a);

        final var ctrl = new ListFleetByPassengerCapacityController(auth, aircraft, models, companies);
        assertThrows(IllegalStateException.class, () -> ctrl.listFleetByPassengerCapacity("33"));
    }
}
