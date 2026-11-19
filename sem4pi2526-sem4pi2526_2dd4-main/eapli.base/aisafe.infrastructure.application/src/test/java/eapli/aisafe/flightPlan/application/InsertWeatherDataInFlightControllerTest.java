package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightPlan.domain.DepartureDateTime;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightLegId;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanDesignator;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import eapli.aisafe.flightPlan.domain.FlightSegmentId;
import eapli.aisafe.flightPlan.domain.FlightType;
import eapli.aisafe.flightPlan.domain.FlightWind;
import eapli.aisafe.flightPlan.domain.FuelPlan;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.aisafe.flightroute.RouteDistance;
import eapli.aisafe.flightroute.RouteDuration;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.pilot.License;
import eapli.aisafe.pilot.PilotCertification;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.domain.model.Role;

import org.junit.Before;
import org.junit.Test;

public class InsertWeatherDataInFlightControllerTest {

    private FakeAuth auth;
    private TestFlightPlanRepo flightPlanRepo;
    private TestWeatherDataRepo weatherDataRepo;
    private TestFlightRouteRepo flightRouteRepo;
    private TestAirportRepo airportRepo;
    private TestAirControlAreaRepo airControlAreaRepo;
    private TestPilotRepo pilotRepo;
    private InsertWeatherDataInFlightController controller;

    private FlightPlan flightPlan;
    private FlightPlanId flightPlanId;
    private FlightLegId legId;
    private PilotId pilotId;

    @Before
    public void setUp() {
        auth = new FakeAuth();
        flightPlanRepo = new TestFlightPlanRepo();
        weatherDataRepo = new TestWeatherDataRepo();
        flightRouteRepo = new TestFlightRouteRepo();
        airportRepo = new TestAirportRepo();
        airControlAreaRepo = new TestAirControlAreaRepo();
        pilotRepo = new TestPilotRepo();
        controller = new InsertWeatherDataInFlightController(
                auth, flightPlanRepo, weatherDataRepo,
                flightRouteRepo, airportRepo, airControlAreaRepo, pilotRepo);

        pilotId = PilotId.newId();
        final AirControlArea lppc = new AirControlArea("LPPC", "Lisboa FIR", 30, 50, -20, 0);
        airControlAreaRepo.store.put(new AirControlAreaCode("LPPC"), lppc);

        final Airport lis = new Airport("LPPT", "LIS", "Lisbon Portela",
                "Lisbon", "Portugal", 38.78, -9.14, 114, lppc);
        airportRepo.store.put("LPPT", lis);

        final Airport fnc = new Airport("LPMA", "FNC", "Madeira Cristiano Ronaldo",
                "Funchal", "Portugal", 32.70, -16.77, 59, lppc);
        airportRepo.store.put("LPMA", fnc);

        final FlightRoute route = FlightRoute.create(
                new FlightRouteName("TP123"),
                CompanyId.newId(),
                AirportId.forIcaoCode("LPPT"),
                AirportId.forIcaoCode("LPMA"),
                new RouteDistance(600, MeasurementUnit.MILE),
                new RouteDuration(120));
        flightRouteRepo.store.put(route.identity(), route);

        legId = FlightLegId.newId();
        final FlightLeg leg = new FlightLeg(legId,
                AirportId.forIcaoCode("LPPT"),
                AirportId.forIcaoCode("LPMA"),
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));

        final FlightRouteId routeId = route.identity();
        flightPlanId = FlightPlanId.newId();
        flightPlan = new FlightPlan(
                flightPlanId,
                RegistrationNumber.valueOf("CS-TTK"),
                routeId,
                new FlightPlanDesignator("FP001"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30)),
                List.of(leg),
                new FlightWind(new WindSpeed(10, "m/s"), new WindDirection(180), MeasurementUnit.KILOGRAM));
        flightPlan.setPilot(pilotId);
        flightPlanRepo.store.put(flightPlanId, flightPlan);

        final Pilot pilot = new Pilot(pilotId, new FullName("John", "Doe"),
                new Email("pilot1@test.com"), CompanyId.newId(),
                new PilotCertification("ATP-FAA"),
                new License("LIC-001", LocalDateTime.now().plusYears(1)),
                List.of(AircraftModelId.newId()));
        pilotRepo.store.put(pilotId, pilot);
    }

    @Test
    public void shouldInsertWeatherDataSuccessfully() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC",
                Instant.parse("2026-06-10T12:00:00Z"),
                12.5, 90.0, 20.0, 55.0);

        controller.insertWeatherData(dto, pilotId);

        final FlightPlan saved = flightPlanRepo.store.get(flightPlanId);
        assertNotNull(saved);
        assertNotNull(saved.getLegs().get(0).weatherData());
        assertEquals(12.5, saved.getLegs().get(0).weatherData().windSpeedMs(), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenFlightPlanNotFound() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                UUID.randomUUID().toString(),
                legId.toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenLegNotFound() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                FlightLegId.newId().toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotDoesNotMatch() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, PilotId.newId());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenFlightPlanIsCancelled() {
        flightPlan.setStatus(FlightPlanStatus.CANCELLED);
        flightPlanRepo.store.put(flightPlanId, flightPlan);

        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test
    public void shouldRevertToPendingTestWhenTested() {
        flightPlan.setStatus(FlightPlanStatus.TESTED);
        flightPlanRepo.store.put(flightPlanId, flightPlan);

        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                8.0, 45.0, 22.0, 60.0);

        controller.insertWeatherData(dto, pilotId);

        final FlightPlan saved = flightPlanRepo.store.get(flightPlanId);
        assertEquals(FlightPlanStatus.PENDING_TEST, saved.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAreaCodeNotFound() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "XXXX", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenDateTooFarFromDeparture() {
        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC",
                Instant.parse("2026-06-01T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test(expected = SecurityException.class)
    public void shouldThrowWhenNotAuthenticated() {
        auth.throwOnNextCall = true;

        final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                flightPlanId.toString(),
                legId.toString(),
                "LPPC", Instant.parse("2026-06-10T12:00:00Z"),
                10, 180, 25, 50);

        controller.insertWeatherData(dto, pilotId);
    }

    @Test
    public void shouldListPlansForCurrentPilot() {
        final List<FlightPlan> plans = controller.listPlansForCurrentPilot();
        assertEquals(1, plans.size());
        assertEquals(flightPlanId, plans.get(0).identity());
    }

    @Test
    public void shouldListLegsForPlan() {
        final List<FlightLeg> legs = controller.listLegsForPlan(flightPlanId);
        assertEquals(1, legs.size());
        assertEquals(legId, legs.get(0).identity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowListLegsForWrongPilot() {
        final FlightPlan other = new FlightPlan(
                FlightPlanId.newId(),
                RegistrationNumber.valueOf("CS-TTK"),
                flightPlan.getFlightRoute(),
                new FlightPlanDesignator("FP002"),
                FlightType.COMMERCIAL,
                flightPlan.getDepartureDateTime(),
                flightPlan.getLegs(),
                flightPlan.getWind());
        other.setPilot(PilotId.newId());
        flightPlanRepo.store.put(other.identity(), other);

        controller.listLegsForPlan(other.identity());
    }

    // ------------------------------------------------------------------
    // Stub implementations
    // ------------------------------------------------------------------

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;
        boolean throwOnNextCall;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
            if (throwOnNextCall) {
                throwOnNextCall = false;
                throw new SecurityException("Not authenticated");
            }
        }

        @Override
        public String currentUsername() {
            return "pilot1@test.com";
        }
    }

    private static final class TestWeatherDataRepo implements WeatherDataRepository {
        final Map<WeatherDataId, WeatherData> store = new HashMap<>();

        @Override
        public WeatherData save(final WeatherData entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<WeatherData> ofIdentity(final WeatherDataId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<WeatherData> findAll() {
            return store.values();
        }

        @Override
        public void delete(final WeatherData entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final WeatherDataId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
            return List.of();
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                           final Instant fromInclusive,
                                                           final Instant toExclusive) {
            return List.of();
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
        public Optional<FlightRoute> findById(final String id) {
            return store.values().stream()
                    .filter(r -> r.identity().toString().equals(id))
                    .findFirst();
        }

        @Override
        public boolean existsByName(final FlightRouteName name) {
            return store.values().stream().anyMatch(r -> r.getName().equals(name));
        }
    }

    private static final class TestAirportRepo implements AirportRepository {
        final Map<String, Airport> store = new HashMap<>();

        @Override
        public Airport save(final Airport entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Airport> ofIdentity(final String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<Airport> findAll() {
            return store.values();
        }

        @Override
        public void delete(final Airport entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final String id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return ofIdentity(icaoCode);
        }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return store.values().stream()
                    .filter(a -> a.iataCode().equals(iataCode))
                    .findFirst();
        }

        @Override
        public Optional<Airport> findById(final String code) {
            return ofIdentity(code);
        }

        @Override
        public Boolean exists(final AirportId id) {
            return store.containsKey(id.code());
        }
    }

    private static final class TestAirControlAreaRepo implements AirControlAreaRepository {
        final Map<AirControlAreaCode, AirControlArea> store = new HashMap<>();

        @Override
        public AirControlArea save(final AirControlArea entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirControlArea> ofIdentity(final AirControlAreaCode id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AirControlArea> findAll() {
            return store.values();
        }

        @Override
        public void delete(final AirControlArea entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final AirControlAreaCode id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
            return ofIdentity(code);
        }

        @Override
        public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
            return Optional.empty();
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
            return store.values().stream()
                    .filter(p -> p.identity().toString().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<Pilot> findByEmail(final Email email) {
            return store.values().stream()
                    .filter(p -> p.getEmail().value().equals(email.value()))
                    .findFirst();
        }

        @Override
        public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
            return List.of();
        }
    }
}
