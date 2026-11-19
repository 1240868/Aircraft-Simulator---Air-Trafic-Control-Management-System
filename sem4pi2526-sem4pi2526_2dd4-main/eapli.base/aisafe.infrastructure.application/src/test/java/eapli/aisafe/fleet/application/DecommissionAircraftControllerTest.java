package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.FakeAuth;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.FakePendingFlights;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestAircraftRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.roles.Roles;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

/** US071 — retire aircraft owned by the collaborator's company. */
public class DecommissionAircraftControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new DecommissionAircraftController());
    }

    private static Aircraft sampleAircraft(final CompanyId owner) {
        return AircraftFactory.registerNew(
                "CS-TST", "PT", AircraftModelId.newId(), owner,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100,
                LocalDate.of(2015, 1, 1), null);
    }

    @Test
    public void decommissionShouldSucceedWhenOwnedAndNoPendingFlights() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var aircraft = new TestAircraftRepo();
        final var pending = new FakePendingFlights();
        final CompanyId company = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(company);
        final Aircraft ac = sampleAircraft(company);
        aircraft.save(ac);
        final var ctrl = new DecommissionAircraftController(auth, aircraft, companies, pending);

        ctrl.decommission("  cs-tst  ");

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertTrue(aircraft.saved.get(aircraft.saved.size() - 1).isRetired());
    }

    @Test
    public void decommissionShouldSucceedWhenPendingFlightsAreForAnotherTail() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var aircraft = new TestAircraftRepo();
        final var pending = new FakePendingFlights();
        final CompanyId company = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(company);
        final Aircraft ac = sampleAircraft(company);
        aircraft.save(ac);
        pending.pending.add(RegistrationNumber.valueOf("CS-OTH"));
        final var ctrl = new DecommissionAircraftController(auth, aircraft, companies, pending);

        ctrl.decommission("CS-TST");

        assertTrue(aircraft.saved.get(aircraft.saved.size() - 1).isRetired());
    }

    @Test
    public void decommissionShouldFailWhenRegistrationInvalid() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new DecommissionAircraftController(auth, new TestAircraftRepo(), companies,
                new FakePendingFlights());

        assertThrows(IllegalArgumentException.class, () -> ctrl.decommission("!!!"));
    }

    @Test
    public void decommissionShouldFailWhenUserNotLinkedToCompany() {
        final var ctrl = new DecommissionAircraftController(new FakeAuth(), new TestAircraftRepo(),
                new TestCompanyRepo(), new FakePendingFlights());

        assertThrows(IllegalStateException.class, () -> ctrl.decommission("CS-TST"));
    }

    @Test
    public void decommissionShouldFailWhenAircraftNotFound() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(new CompanyId(UUID.randomUUID()));
        final var ctrl = new DecommissionAircraftController(auth, new TestAircraftRepo(), companies,
                new FakePendingFlights());

        assertThrows(IllegalArgumentException.class, () -> ctrl.decommission("CS-XXX"));
    }

    @Test
    public void decommissionShouldFailWhenAircraftBelongsToAnotherCompany() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId mine = new CompanyId(UUID.randomUUID());
        final CompanyId other = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(mine);
        final Aircraft ac = sampleAircraft(other);
        aircraft.save(ac);
        final var ctrl = new DecommissionAircraftController(auth, aircraft, companies, new FakePendingFlights());

        assertThrows(IllegalArgumentException.class, () -> ctrl.decommission("CS-TST"));
    }

    @Test
    public void decommissionShouldFailWhenPendingFlights() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var aircraft = new TestAircraftRepo();
        final var pending = new FakePendingFlights();
        final CompanyId company = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(company);
        final Aircraft ac = sampleAircraft(company);
        aircraft.save(ac);
        pending.pending.add(RegistrationNumber.valueOf("CS-TST"));
        final var ctrl = new DecommissionAircraftController(auth, aircraft, companies, pending);

        assertThrows(IllegalStateException.class, () -> ctrl.decommission("CS-TST"));
    }

    @Test
    public void decommissionShouldFailWhenAlreadyRetired() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final var aircraft = new TestAircraftRepo();
        final CompanyId company = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(company);
        final Aircraft ac = sampleAircraft(company);
        ac.markRetired();
        aircraft.save(ac);
        final var ctrl = new DecommissionAircraftController(auth, aircraft, companies, new FakePendingFlights());

        assertThrows(IllegalStateException.class, () -> ctrl.decommission("CS-TST"));
    }
}
