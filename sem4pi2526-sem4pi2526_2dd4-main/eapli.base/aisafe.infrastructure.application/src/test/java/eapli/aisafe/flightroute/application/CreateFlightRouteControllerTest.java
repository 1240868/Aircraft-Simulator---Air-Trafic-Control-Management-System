package eapli.aisafe.flightroute.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.domain.OperationalStatus;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.aisafe.flightroute.RouteActivationStatus;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/** US073 — create flight route for collaborator company. */
public class CreateFlightRouteControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new CreateFlightRouteController());
    }

    @Test
    public void createRouteShouldPersistActiveRouteWithCompanyPrefix() {
        final var auth = new FakeAuth();
        final var companies = new TestCompanyRepo();
        final CompanyId companyId = seedCompany(companies, "TP");
        companies.linkedCompany = Optional.of(companyId);

        final var airports = new TestAirportRepo();
        airports.seed(
                airport("LPPT", "LIS", 38.77, -9.13),
                airport("LPPR", "OPO", 41.25, -8.68));

        final var routes = new TestFlightRouteRepo();
        final var ctrl = new CreateFlightRouteController(auth, routes, airports, companies);

        final FlightRouteDTO dto = ctrl.createRoute("123", "LPPT", "LPPR");

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals("TP123", dto.routeName());
        assertEquals("TP123", routes.lastSaved.getName().value());
        assertEquals(RouteActivationStatus.ACTIVE, routes.lastSaved.getStatus());
        assertEquals("LPPT", routes.lastSaved.originAirportId().code());
        assertEquals("LPPR", routes.lastSaved.destinationAirportId().code());
        assertTrue(dto.distanceKm() > 0);
        assertTrue(dto.durationMinutes() > 0);
    }

    @Test
    public void createRouteShouldRejectDuplicateName() {
        final var companies = new TestCompanyRepo();
        final CompanyId companyId = seedCompany(companies, "TP");
        companies.linkedCompany = Optional.of(companyId);

        final var airports = new TestAirportRepo();
        airports.seed(airport("LPPT", "LIS", 38.77, -9.13), airport("LPPR", "OPO", 41.25, -8.68));

        final var routes = new TestFlightRouteRepo();
        routes.existingName = new FlightRouteName("TP999");

        final var ctrl = new CreateFlightRouteController(new FakeAuth(), routes, airports, companies);

        assertThrows(IntegrityViolationException.class, () -> ctrl.createRoute("999", "LPPT", "LPPR"));
    }

    @Test
    public void createRouteShouldRejectSameOriginAndDestination() {
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(seedCompany(companies, "TP"));
        final var airports = new TestAirportRepo();
        airports.seed(airport("LPPT", "LIS", 38.77, -9.13));
        final var ctrl = new CreateFlightRouteController(new FakeAuth(), new TestFlightRouteRepo(), airports, companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.createRoute("1", "LPPT", "LPPT"));
    }

    @Test
    public void listOperationalAirportsShouldReturnOnlyActiveSortedByIcao() throws Exception {
        final var auth = new FakeAuth();
        final var airports = new TestAirportRepo();
        airports.seed(
                airport("LPPR", "OPO", 41.25, -8.68),
                airport("LPPT", "LIS", 38.77, -9.13));
        airports.seedClosed(airport("LPFR", "FAO", 37.01, -7.97));

        final var ctrl = new CreateFlightRouteController(
                auth, new CreateFlightRouteService(new TestFlightRouteRepo(), airports, new TestCompanyRepo()), airports);

        final var listed = ctrl.listOperationalAirports();

        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(2, listed.size());
        assertEquals("LPPR", listed.get(0).icaoCode());
        assertEquals("LPPT", listed.get(1).icaoCode());
    }

    @Test
    public void createRouteShouldRejectNonOperationalAirport() throws Exception {
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(seedCompany(companies, "TP"));
        final var airports = new TestAirportRepo();
        airports.seedClosed(airport("LPPT", "LIS", 38.77, -9.13));
        airports.seed(airport("LPPR", "OPO", 41.25, -8.68));
        final var ctrl = new CreateFlightRouteController(new FakeAuth(), new TestFlightRouteRepo(), airports, companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.createRoute("1", "LPPT", "LPPR"));
    }

    @Test
    public void createRouteShouldRejectUnknownAirport() {
        final var companies = new TestCompanyRepo();
        companies.linkedCompany = Optional.of(seedCompany(companies, "TP"));
        final var ctrl = new CreateFlightRouteController(
                new FakeAuth(), new TestFlightRouteRepo(), new TestAirportRepo(), companies);

        assertThrows(IllegalArgumentException.class, () -> ctrl.createRoute("1", "LPPT", "LPPR"));
    }

    private static CompanyId seedCompany(final TestCompanyRepo repo, final String iata) {
        final var company = new AirTransportCompany(
                new CompanyName("Test Air"),
                new CompanyIataCode(iata),
                new CompanyIcaoCode("TST"),
                new AirOperatorCertificate("CERT-1", LocalDate.now().plusYears(1)));
        repo.save(company);
        company.addCollaborator("atcc");
        repo.save(company);
        return company.identity();
    }

    private static Airport airport(final String icao, final String iata,
                                   final double lat, final double lon) {
        return new Airport(icao, iata, "Airport " + icao, "City", "PT",
                lat, lon, 100, new AirControlArea("PT-AREA", "Area", 40, 42, -9, -7));
    }

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return "atcc";
        }
    }

    private static final class TestCompanyRepo extends InMemoryDomainRepository<AirTransportCompany, CompanyId>
            implements AirTransportCompanyRepository {

        Optional<CompanyId> linkedCompany = Optional.empty();

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            return linkedCompany;
        }

        @Override
        public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            return Optional.empty();
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            for (final var c : findAll()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }
    }

    private static final class TestAirportRepo extends InMemoryDomainRepository<Airport, String>
            implements AirportRepository {

        private final java.util.Map<String, Airport> localStore = new java.util.HashMap<>();

        void seed(final Airport... items) {
            for (final Airport a : items) {
                save(a);
                localStore.put(a.identity(), a);
            }
        }

        void seedClosed(final Airport airport) throws Exception {
            final Field status = Airport.class.getDeclaredField("operationalStatus");
            status.setAccessible(true);
            status.set(airport, OperationalStatus.CLOSED);
            seed(airport);
        }

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return Optional.ofNullable(localStore.get(icaoCode.trim().toUpperCase()));
        }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Boolean exists(final AirportId id) {
            return localStore.containsKey(id.toString());
        }
    }

    private static final class TestFlightRouteRepo extends InMemoryDomainRepository<FlightRoute, FlightRouteId>
            implements FlightRouteRepository {

        FlightRoute lastSaved;
        FlightRouteName existingName;

        @Override
        public FlightRoute save(final FlightRoute entity) {
            lastSaved = entity;
            return super.save(entity);
        }

        public Optional<FlightRoute> findByName(final FlightRouteName name) {
            if (existingName != null && existingName.value().equals(name.value())) {
                return Optional.empty();
            }
            return Optional.empty();
        }

        @Override
        public Optional<FlightRoute> findById(final String id) {
            return Optional.empty();
        }

        @Override
        public boolean existsByName(final FlightRouteName name) {
            return existingName != null && existingName.value().equals(name.value());
        }
    }
}
