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

/** US072b — list company's active fleet filtered by manufacturer. */
public class ListFleetByMakerControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ListFleetByMakerController());
    }

    private static final LocalDate MFG = LocalDate.of(2015, 4, 1);

    private static AircraftModel airbusModel() {
        return new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
    }

    @Test
    public void listShouldReturnOnlyMatchingMakerSortedByRegistration() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel airbus = airbusModel();
        models.save(airbus);
        final AircraftModelId airId = airbus.identity();

        aircraft.byCompany.add(AircraftFactory.registerNew("CS-BBB", "PT", airId, cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null));
        aircraft.byCompany.add(AircraftFactory.registerNew("CS-AAA", "PT", airId, cid,
                List.of(new CabinSeatBlock("ECONOMY", 80)), 180, MFG, null));

        final var ctrl = new ListFleetByMakerController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByMaker("airbus");

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(2, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
        assertEquals("CS-AAA", r.aircraft().get(0).registrationNumber());
        assertEquals("CS-BBB", r.aircraft().get(1).registrationNumber());
    }

    @Test
    public void listShouldIncludeMaintenanceAircraft() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel airbus = airbusModel();
        models.save(airbus);
        aircraft.byCompany.add(AircraftFactory.registerWithOperationalStatus(
                "CS-MNT", "PT", airbus.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 40)), 180, MFG, null,
                AircraftOperationalStatus.MAINTENANCE));

        final var ctrl = new ListFleetByMakerController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByMaker("Airbus");

        assertEquals(1, r.aircraft().size());
        assertEquals("MAINTENANCE", r.aircraft().get(0).operationalStatus());
    }

    @Test
    public void listShouldReturnEmptyWithMessageWhenNoAircraft() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        models.save(new AircraftModel(
                "E190", "Embraer", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 100, 3000, List.of(EngineModelId.newId())));
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), models, companies);

        final FleetListResult r = ctrl.listFleetByMaker("Embraer");

        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_MAKER, r.messageWhenEmpty());
    }

    @Test
    public void listShouldFailWhenMakerUnknown() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByMaker("UnknownMfr"));
    }

    @Test
    public void listShouldFailWhenUserNotLinkedToCompany() {
        final var auth = new FakeAuth();
        final var models = new TestModelRepo();
        models.save(airbusModel());
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), models, new TestCompanyRepo());

        assertThrows(IllegalStateException.class, () -> ctrl.listFleetByMaker("Airbus"));
    }

    @Test
    public void listShouldFailWhenManufacturerNull() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByMaker(null));
    }

    @Test
    public void listShouldFailWhenManufacturerBlank() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var models = new TestModelRepo();
        models.save(airbusModel());
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), models, companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByMaker("   "));
    }

    @Test
    public void listShouldAcceptWhitespaceAroundMaker() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var models = new TestModelRepo();
        models.save(new AircraftModel(
                "737", "Boeing", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId())));
        final var ctrl = new ListFleetByMakerController(auth, new TestAircraftRepo(), models, companies);

        final FleetListResult r = ctrl.listFleetByMaker("  boeing  ");
        assertNotNull(r);
    }

    @Test
    public void listShouldSkipAircraftWhenModelRecordMissing() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel airbus = airbusModel();
        models.save(new AircraftModel(
                "A330", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 250, 7000, List.of(EngineModelId.newId())));
        aircraft.byCompany.add(AircraftFactory.registerNew(
                "CS-ZZZ", "PT", airbus.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 50)), 180, MFG, null));

        final var ctrl = new ListFleetByMakerController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByMaker("Airbus");

        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_MAKER, r.messageWhenEmpty());
    }

    @Test
    public void listShouldSkipRetiredAndUnknownModel() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel emb = new AircraftModel(
                "E190", "Embraer", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 100, 3000, List.of(EngineModelId.newId()));
        models.save(emb);

        aircraft.byCompany.add(AircraftFactory.registerNew(
                "CS-OK1", "PT", emb.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 80)), 100, MFG, null));
        aircraft.byCompany.add(AircraftFactory.registerWithOperationalStatus(
                "CS-OLD", "PT", emb.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 20)), 100, MFG, null,
                AircraftOperationalStatus.RETIRED));
        aircraft.byCompany.add(AircraftFactory.registerNew(
                "CS-ORP", "PT", AircraftModelId.newId(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 200, MFG, null));

        final var ctrl = new ListFleetByMakerController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByMaker("Embraer");

        assertEquals(1, r.aircraft().size());
        assertEquals("CS-OK1", r.aircraft().get(0).registrationNumber());
    }
}
