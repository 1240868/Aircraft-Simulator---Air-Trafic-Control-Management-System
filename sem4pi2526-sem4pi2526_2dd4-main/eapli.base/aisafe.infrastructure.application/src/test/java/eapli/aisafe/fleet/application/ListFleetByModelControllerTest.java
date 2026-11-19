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

/** US072a — list company's active fleet filtered by aircraft model. */
public class ListFleetByModelControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ListFleetByModelController());
    }

    private static AircraftModel sampleModel() {
        return new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
    }

    @Test
    public void listShouldReturnRowsSortedByRegistrationWithModelDetails() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        final AircraftModelId mid = model.identity();
        models.save(model);

        final LocalDate mfg = LocalDate.of(2016, 1, 1);
        final Aircraft a2 = AircraftFactory.registerNew("CS-BBB", "PT", mid, cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, mfg, null);
        final Aircraft a1 = AircraftFactory.registerNew("CS-AAA", "PT", mid, cid,
                List.of(new CabinSeatBlock("ECONOMY", 50), new CabinSeatBlock("BUSINESS", 20)), 180, mfg, null);
        aircraft.activeByModel.add(a2);
        aircraft.activeByModel.add(a1);

        final var ctrl = new ListFleetByModelController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByModel(mid.toString());

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(2, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
        assertEquals("CS-AAA", r.aircraft().get(0).registrationNumber());
        assertEquals("CS-BBB", r.aircraft().get(1).registrationNumber());
        assertEquals("A320", r.aircraft().get(0).modelName());
        assertEquals("Airbus", r.aircraft().get(0).manufacturer());
        assertEquals("ACTIVE", r.aircraft().get(0).operationalStatus());
        assertEquals(70, r.aircraft().get(0).numberOfSeats());
        assertEquals(100, r.aircraft().get(1).numberOfSeats());
    }

    @Test
    public void listShouldIncludeMaintenanceButExcludeRetiredFromRepository() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        models.save(model);
        final AircraftModelId mid = model.identity();

        final Aircraft maintenance = AircraftFactory.registerWithOperationalStatus(
                "CS-MNT", "PT", mid, cid,
                List.of(new CabinSeatBlock("ECONOMY", 40)),
                180,
                LocalDate.of(2015, 1, 1),
                null,
                AircraftOperationalStatus.MAINTENANCE);
        aircraft.activeByModel.add(maintenance);

        final var ctrl = new ListFleetByModelController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByModel(mid.toString());

        assertEquals(1, r.aircraft().size());
        assertEquals("MAINTENANCE", r.aircraft().get(0).operationalStatus());
    }

    @Test
    public void listShouldReturnEmptyWithMessageWhenNoAircraft() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final AircraftModel model = sampleModel();
        models.save(model);

        final var ctrl = new ListFleetByModelController(auth, aircraft, models, companies);
        final FleetListResult r = ctrl.listFleetByModel(model.identity().toString());

        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_MODEL, r.messageWhenEmpty());
    }

    @Test
    public void listShouldFailWhenUserNotLinkedToCompany() {
        final var auth = new FakeAuth();
        final var models = new TestModelRepo();
        models.save(sampleModel());
        final var ctrl = new ListFleetByModelController(auth, new TestAircraftRepo(), models, new TestCompanyRepo());

        assertThrows(IllegalStateException.class,
                () -> ctrl.listFleetByModel(models.findAll().iterator().next().identity().toString()));
    }

    @Test
    public void listShouldFailWhenModelUnknown() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByModelController(auth, new TestAircraftRepo(), new TestModelRepo(), companies);

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.listFleetByModel(UUID.randomUUID().toString()));
    }

    @Test
    public void listShouldWrapInvalidUuidAsIllegalArgument() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var models = new TestModelRepo();
        models.save(sampleModel());
        final var ctrl = new ListFleetByModelController(auth, new TestAircraftRepo(), models, companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByModel("not-a-uuid"));
    }

    @Test
    public void listShouldAcceptModelIdWithWhitespace() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var models = new TestModelRepo();
        final AircraftModel model = sampleModel();
        models.save(model);
        final var ctrl = new ListFleetByModelController(auth, new TestAircraftRepo(), models, companies);

        final FleetListResult r = ctrl.listFleetByModel("  " + model.identity().toString() + "  ");
        assertNotNull(r);
    }
}
