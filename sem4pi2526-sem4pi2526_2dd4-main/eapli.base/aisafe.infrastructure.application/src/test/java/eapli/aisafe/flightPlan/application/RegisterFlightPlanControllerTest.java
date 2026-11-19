package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.aisafe.flightroute.RouteActivationStatus;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.License;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotCertification;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.flightroute.RouteDistance;
import eapli.aisafe.flightroute.RouteDuration;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.framework.infrastructure.authz.domain.model.Role;

import org.junit.Before;
import org.junit.Test;

public class RegisterFlightPlanControllerTest {

    private FakeAuth auth;
    private TestFlightRouteRepo routeRepo;
    private TestAircraftRepo aircraftRepo;
    private TestPilotRepo pilotRepo;
    private TestFlightPlanRepo flightPlanRepo;
    private TestAirTransportCompanyRepo companyRepo;
    private TestAircraftModelRepo aircraftModelRepo;
    private RegisterFlightPlanController controller;

    private FlightRoute activeRoute;
    private FlightRouteId routeId;
    private Aircraft aircraft;
    private RegistrationNumber aircraftReg;
    private Pilot pilot;
    private PilotId pilotId;
    private CompanyId companyId;
    private AircraftModelId modelId;
    private List<AircraftModelId> modelIds;

    @Before
    public void setUp() throws Exception {
        auth = new FakeAuth();
        routeRepo = new TestFlightRouteRepo();
        aircraftRepo = new TestAircraftRepo();
        pilotRepo = new TestPilotRepo();
        flightPlanRepo = new TestFlightPlanRepo();
        companyRepo = new TestAirTransportCompanyRepo();
        aircraftModelRepo = new TestAircraftModelRepo();
        controller = new RegisterFlightPlanController(auth, flightPlanRepo, routeRepo, aircraftRepo, pilotRepo,
                companyRepo, aircraftModelRepo);

        companyId = new CompanyId(UUID.randomUUID());
        modelId = new AircraftModelId(UUID.randomUUID());

        final var company = new AirTransportCompany(
                new CompanyName("Test Airlines"),
                new CompanyIataCode("FP"),
                new eapli.aisafe.company.domain.CompanyIcaoCode("FPL"),
                new eapli.aisafe.company.domain.AirOperatorCertificate("AOC-TEST", LocalDate.now().plusYears(1)));
        companyRepo.store.put(companyId, company);

        final var engineModelId = new EngineModelId(UUID.randomUUID());
        final var aircraftModel = new AircraftModel(
                "TestModel", "TestMfr", AircraftType.PASSENGER, EngineType.TURBOFAN, 2, 150, 5000.0, List.of(engineModelId));
        aircraftModelRepo.store.put(aircraftModel.identity(), aircraftModel);

        aircraft = AircraftFactory.registerNew(
                "CS-TTK", "PT", modelId, companyId,
                List.of(new CabinSeatBlock("Economy", 150)),
                200, LocalDate.of(2012, 3, 3), LocalDate.of(2012, 6, 1));
        aircraftReg = aircraft.identity();
        aircraftRepo.store.put(aircraftReg, aircraft);

        activeRoute = FlightRoute.create(
                new FlightRouteName("RT001"),
                companyId,
                AirportId.newId(),
                AirportId.newId(),
                new RouteDistance(100, MeasurementUnit.METER),
                new RouteDuration(60));
        routeId = activeRoute.identity();
        routeRepo.store.put(routeId, activeRoute);

        pilotId = PilotId.newId();
        modelIds = List.of(aircraftModel.identity());
        pilot = new Pilot(pilotId, new FullName("John", "Doe"),
                new Email("john@example.com"), companyId,
                new PilotCertification("ATP-FAA"),
                new License("LIC-001", LocalDateTime.now().plusYears(1)),
                modelIds);
        pilotRepo.store.put(pilotId, pilot);
    }

    @Test
    public void shouldRegisterFlightPlanWithValidData() {
        final FlightPlan result = controller.register(
                routeId, aircraftReg, pilotId, "FP001",
                "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
        assertNotNull(result);
        assertNotNull(result.identity());
        assertEquals(pilotId, result.getPilot());
        assertTrue(auth.lastRoles.length > 0);
    }

    @Test
    public void listPilotsShouldReturnOnlyActivePilots() throws Exception {
        final Pilot inactive = new Pilot(PilotId.newId(), new FullName("Jane", "Doe"),
                new Email("jane@example.com"), companyId,
                new PilotCertification("ATP-FAA"),
                new License("LIC-INACTIVE", LocalDateTime.now().plusYears(1)),
                modelIds);
        final Field statusField = Pilot.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(inactive, PilotStatus.INACTIVE);
        pilotRepo.store.put(inactive.identity(), inactive);

        final List<Pilot> listed = new ArrayList<>();
        for (final Pilot p : controller.listPilots()) {
            listed.add(p);
        }

        assertEquals(1, listed.size());
        assertEquals(pilotId, listed.get(0).identity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenRouteNotFound() {
        controller.register(FlightRouteId.newId(), aircraftReg, pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenRouteNotActive() throws Exception {
        final Field statusField = FlightRoute.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(activeRoute, RouteActivationStatus.INACTIVE);
        controller.register(routeId, aircraftReg, pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAircraftNotFound() {
        controller.register(routeId, RegistrationNumber.valueOf("XX-XXXX"), pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotNotFound() {
        controller.register(routeId, aircraftReg, PilotId.newId(),
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotInactive() throws Exception {
        final Pilot inactive = new Pilot(PilotId.newId(), new FullName("Jane", "Doe"),
                new Email("jane@example.com"), companyId,
                new PilotCertification("ATP-FAA"),
                new License("LIC-EXPIRED", LocalDateTime.now().plusYears(1)),
                modelIds);
        final Field statusField = Pilot.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(inactive, PilotStatus.INACTIVE);

        final PilotId inactiveId = inactive.identity();
        pilotRepo.store.put(inactiveId, inactive);

        controller.register(routeId, aircraftReg, inactiveId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotCompanyDoesNotMatchRouteCompany() {
        final Pilot otherPilot = new Pilot(PilotId.newId(), new FullName("Other", "Pilot"),
                new Email("other@example.com"), new CompanyId(UUID.randomUUID()),
                new PilotCertification("ATP-FAA"),
                new License("LIC-OTHER", LocalDateTime.now().plusYears(1)),
                modelIds);
        final PilotId otherId = otherPilot.identity();
        pilotRepo.store.put(otherId, otherPilot);

        controller.register(routeId, aircraftReg, otherId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test
    public void listFlightRoutesShouldReturnAll() {
        final Iterable<FlightRoute> routes = controller.listFlightRoutes();
        final List<FlightRoute> list = new ArrayList<>();
        routes.forEach(list::add);
        assertEquals(1, list.size());
        assertEquals(routeId, list.get(0).identity());
    }

    @Test
    public void listAircraftShouldReturnAll() {
        final Iterable<Aircraft> list = controller.listAircraft();
        final List<Aircraft> items = new ArrayList<>();
        list.forEach(items::add);
        assertEquals(1, items.size());
        assertEquals(aircraftReg, items.get(0).identity());
    }

    @Test
    public void listPilotsShouldReturnAll() {
        final Iterable<Pilot> list = controller.listPilots();
        final List<Pilot> items = new ArrayList<>();
        list.forEach(items::add);
        assertEquals(1, items.size());
        assertEquals(pilotId, items.get(0).identity());
    }

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;
        String username = "pilot1";

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    private static final class TestFlightPlanRepo implements FlightPlanRepository {
        final Map<FlightPlanId, FlightPlan> store = new HashMap<>();

        @Override
        public FlightPlan save(final FlightPlan entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<FlightPlan> ofIdentity(final FlightPlanId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<FlightPlan> findAll() {
            return store.values();
        }

        @Override
        public Optional<FlightPlan> findById(final FlightPlanId flightPlanId) {
            return ofIdentity(flightPlanId);
        }

        @Override
        public void delete(final FlightPlan entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightPlanId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

    @Override
    public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
        return store.values().stream().anyMatch(fp -> fp.getPilot().equals(pilotId));
    }

    @Override
    public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
        return List.of();
    }
}

    private static final class TestFlightRouteRepo implements FlightRouteRepository {
        final Map<FlightRouteId, FlightRoute> store = new HashMap<>();

        @Override
        public FlightRoute save(final FlightRoute entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<FlightRoute> ofIdentity(final FlightRouteId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<FlightRoute> findAll() {
            return store.values();
        }

        @Override
        public void delete(final FlightRoute entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightRouteId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public boolean existsByName(final FlightRouteName name) {
            return store.values().stream().anyMatch(r -> r.getName().equals(name));
        }

        @Override
        public Optional<FlightRoute> findById(final String id) {
            return store.values().stream()
                    .filter(r -> r.identity().toString().equals(id))
                    .findFirst();
        }
    }

    private static final class TestAircraftRepo implements AircraftRepository {
        final Map<RegistrationNumber, Aircraft> store = new HashMap<>();

        @Override
        public Aircraft save(final Aircraft entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Aircraft> ofIdentity(final RegistrationNumber id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<Aircraft> findAll() {
            return store.values();
        }

        @Override
        public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
            return ofIdentity(registration);
        }

        @Override
        public Optional<Aircraft> findByRegistration(final String registration) {
            return store.values().stream()
                    .filter(a -> a.identity().toString().equals(registration))
                    .findFirst();
        }

        @Override
        public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
            final List<Aircraft> result = new ArrayList<>();
            for (final Aircraft a : store.values()) {
                if (a.ownerCompanyId().equals(companyId)) {
                    result.add(a);
                }
            }
            return result;
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                                   final AircraftModelId modelId) {
            return List.of();
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                               final int passengerCapacity) {
            return List.of();
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndAge(final CompanyId companyId,
                                                                 final int completeYears,
                                                                 final LocalDate asOf) {
            return List.of();
        }

        @Override
        public void delete(final Aircraft entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final RegistrationNumber id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private static final class TestPilotRepo implements PilotRepository {
        final Map<PilotId, Pilot> store = new HashMap<>();

        @Override
        public Pilot save(final Pilot entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Pilot> ofIdentity(final PilotId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<Pilot> findAll() {
            return store.values();
        }

        @Override
        public void delete(final Pilot entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final PilotId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<Pilot> ofIdentity(final String id) {
            for (final Pilot p : store.values()) {
                if (p.identity().toString().equals(id)) return Optional.of(p);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Pilot> findByEmail(final Email email) {
            return store.values().stream()
                    .filter(p -> p.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
            final List<Pilot> result = new ArrayList<>();
            for (final Pilot p : store.values()) {
                if (p.getCompany().equals(companyId) && p.getLicenseVoo().expirationDate().isAfter(LocalDateTime.now())) {
                    result.add(p);
                }
            }
            return result;
        }
    }

    private static final class TestAirTransportCompanyRepo implements AirTransportCompanyRepository {
        final Map<CompanyId, AirTransportCompany> store = new HashMap<>();

        @Override
        public AirTransportCompany save(final AirTransportCompany entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirTransportCompany> ofIdentity(final CompanyId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AirTransportCompany> findAll() {
            return store.values();
        }

        @Override
        public void delete(final AirTransportCompany entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final CompanyId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
            return store.values().stream()
                    .filter(c -> c.iataCode().value().equals(iataCode))
                    .findFirst();
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            return store.values().stream()
                    .filter(c -> c.icaoCode().value().equals(icaoCode))
                    .findFirst();
        }

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            return Optional.empty();
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            return Optional.empty();
        }
    }

    private static final class TestAircraftModelRepo implements AircraftModelRepository {
        final Map<AircraftModelId, AircraftModel> store = new HashMap<>();

        @Override
        public AircraftModel save(final AircraftModel entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AircraftModel> ofIdentity(final AircraftModelId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AircraftModel> findAll() {
            return store.values();
        }

        @Override
        public void delete(final AircraftModel entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final AircraftModelId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName, final String manufacturer) {
            return Optional.empty();
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            return store.values().stream()
                    .filter(m -> m.identity().toString().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            final var found = ofIdentity(id);
            if (found.isPresent()) return found;
            return store.values().stream().findFirst();
        }
    }
}
