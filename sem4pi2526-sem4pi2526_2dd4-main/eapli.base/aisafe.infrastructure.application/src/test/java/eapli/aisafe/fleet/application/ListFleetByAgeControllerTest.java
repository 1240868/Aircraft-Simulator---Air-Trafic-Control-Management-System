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

/** US072d — list company's active fleet filtered by complete years of age. */
public class ListFleetByAgeControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ListFleetByAgeController());
    }

    private static final LocalDate AS_OF = LocalDate.of(2026, 6, 1);

    private static AircraftModel sampleModel() {
        return new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
    }

    @Test
    public void listShouldReturnOnlyAircraftMatchingExactAgeSortedByRegistration() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        models.save(model);
        final AircraftModelId mid = model.identity();

        aircraft.activeByAge.add(AircraftFactory.registerNew("CS-BBB", "PT", mid, cid,
                List.of(new CabinSeatBlock("ECONOMY", 50)), 180,
                LocalDate.of(2020, 1, 10), null));
        aircraft.activeByAge.add(AircraftFactory.registerNew("CS-AAA", "PT", mid, cid,
                List.of(new CabinSeatBlock("ECONOMY", 40)), 180,
                LocalDate.of(2020, 6, 1), null));

        final var ctrl = new ListFleetByAgeController(auth, aircraft, models, companies, () -> AS_OF);
        final FleetListResult r = ctrl.listFleetByAge("6");

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(2, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
        assertEquals("CS-AAA", r.aircraft().get(0).registrationNumber());
        assertEquals(Integer.valueOf(6), r.aircraft().get(0).ageInCompleteYears());
    }

    @Test
    public void listShouldPreferManufacturingDateForAge() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        models.save(model);
        aircraft.activeByAge.add(AircraftFactory.registerNew("CS-PREF", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 180,
                LocalDate.of(2018, 1, 1), LocalDate.of(2020, 1, 1)));

        final var ctrl = new ListFleetByAgeController(auth, aircraft, models, companies,
                () -> LocalDate.of(2026, 1, 1));
        final FleetListResult r = ctrl.listFleetByAge("8");

        assertEquals(1, r.aircraft().size());
        assertEquals(Integer.valueOf(8), r.aircraft().get(0).ageInCompleteYears());
    }

    @Test
    public void listShouldIncludeMaintenanceButExcludeRetired() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        models.save(model);
        aircraft.activeByAge.add(AircraftFactory.registerWithOperationalStatus(
                "CS-MNT", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 40)), 180,
                LocalDate.of(2015, 1, 1), null,
                AircraftOperationalStatus.MAINTENANCE));

        final var ctrl = new ListFleetByAgeController(auth, aircraft, models, companies,
                () -> LocalDate.of(2026, 1, 1));
        final FleetListResult r = ctrl.listFleetByAge("11");

        assertEquals(1, r.aircraft().size());
        assertEquals("MAINTENANCE", r.aircraft().get(0).operationalStatus());
    }

    @Test
    public void listShouldReturnEmptyWithMessageWhenNoMatches() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        models.save(sampleModel());
        final var ctrl = new ListFleetByAgeController(auth, new TestAircraftRepo(), models, companies, () -> AS_OF);

        final FleetListResult r = ctrl.listFleetByAge("99");

        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_AGE, r.messageWhenEmpty());
    }

    @Test
    public void listShouldFailWhenUserNotLinkedToCompany() {
        final var ctrl = new ListFleetByAgeController(new FakeAuth(), new TestAircraftRepo(),
                new TestModelRepo(), new TestCompanyRepo(), () -> AS_OF);

        assertThrows(IllegalStateException.class, () -> ctrl.listFleetByAge("5"));
    }

    @Test
    public void listShouldFailWhenAgeNull() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByAgeController(auth, new TestAircraftRepo(), new TestModelRepo(), companies,
                () -> AS_OF);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge(null));
    }

    @Test
    public void listShouldFailWhenAgeMissingOrBlank() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByAgeController(auth, new TestAircraftRepo(), new TestModelRepo(), companies,
                () -> AS_OF);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge(null));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge(""));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge("   "));
    }

    @Test
    public void listShouldFailWhenAgeNotPositive() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByAgeController(auth, new TestAircraftRepo(), new TestModelRepo(), companies,
                () -> AS_OF);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge("0"));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge("-3"));
    }

    @Test
    public void listShouldFailWhenAgeNotAnInteger() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new ListFleetByAgeController(auth, new TestAircraftRepo(), new TestModelRepo(), companies,
                () -> AS_OF);

        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge("3.5"));
        assertThrows(IllegalArgumentException.class, () -> ctrl.listFleetByAge("x"));
    }

    @Test
    public void listShouldAcceptAgeWithWhitespace() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var models = new TestModelRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);
        final AircraftModel model = sampleModel();
        models.save(model);
        aircraft.activeByAge.add(AircraftFactory.registerNew("CS-ZZZ", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 55)), 180,
                LocalDate.of(2021, 6, 1), null));

        final var ctrl = new ListFleetByAgeController(auth, aircraft, models, companies,
                () -> LocalDate.of(2026, 6, 1));
        final FleetListResult r = ctrl.listFleetByAge("  5  ");

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
        final AircraftModel model = sampleModel();
        aircraft.activeByAge.add(AircraftFactory.registerNew("CS-ORP", "PT", model.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 33)), 180,
                LocalDate.of(2018, 1, 1), null));

        final var ctrl = new ListFleetByAgeController(auth, aircraft, models, companies,
                () -> LocalDate.of(2026, 1, 1));
        assertThrows(IllegalStateException.class, () -> ctrl.listFleetByAge("8"));
    }
}
