package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.application.security.AuthorizationPort;
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
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.pilot.License;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotCertification;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class TestFlightPlanControllerTest {

    private FakeAuth auth;
    private TestFlightPlanRepo flightPlanRepo;
    private TestPilotRepo pilotRepo;
    private RecordingDslValidator dslValidator;
    private RecordingPhysicsTester physicsTester;
    private TestFlightPlanController controller;

    private FlightPlan flightPlan;
    private FlightPlanId flightPlanId;
    private PilotId pilotId;

    @Before
    public void setUp() {
        auth = new FakeAuth();
        flightPlanRepo = new TestFlightPlanRepo();
        pilotRepo = new TestPilotRepo();
        dslValidator = new RecordingDslValidator();
        physicsTester = new RecordingPhysicsTester();

        pilotId = PilotId.newId();
        final AirportId dep = AirportId.newId();
        final AirportId arr = AirportId.newId();
        final FlightLegId legId = FlightLegId.newId();
        final FlightLeg leg = new FlightLeg(legId, dep, arr, 1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));

        flightPlanId = FlightPlanId.newId();
        flightPlan = new FlightPlan(
                flightPlanId,
                RegistrationNumber.valueOf("CS-TTK"),
                FlightRouteId.newId(),
                new FlightPlanDesignator("FP001"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30)),
                List.of(leg),
                new FlightWind(new WindSpeed(10, "m/s"), new WindDirection(180), MeasurementUnit.KILOGRAM));
        flightPlan.setPilot(pilotId);
        flightPlan.setDslSourcePath("sample.dfdsl");
        flightPlanRepo.store.put(flightPlanId, flightPlan);
        pilotRepo.pilotId = pilotId;
        auth.username = "pilot@test.com";

        controller = new TestFlightPlanController(
                auth,
                flightPlanRepo,
                pilotRepo,
                dslValidator,
                plan -> Files.createTempFile("fp-test-", ".json"),
                physicsTester);
    }

    @Test
    public void shouldPassWhenDslAndPhysicsSucceed() {
        dslValidator.errors = List.of();
        physicsTester.errors = List.of();

        final FlightPlanTestResult result = controller.test(flightPlanId, pilotId);

        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TESTED, result.newStatus());
        assertEquals(FlightPlanStatus.TESTED, flightPlanRepo.store.get(flightPlanId).getStatus());
        assertTrue(auth.checkedPilot);
    }

    @Test
    public void shouldFailWhenDslValidationFails() {
        dslValidator.errors = List.of("syntax error line 1");

        final FlightPlanTestResult result = controller.test(flightPlanId, pilotId);

        assertFalse(result.passed());
        assertEquals(FlightPlanStatus.REJECTED, result.newStatus());
        assertEquals(List.of("syntax error line 1"), result.dslErrors());
        assertEquals(FlightPlanStatus.REJECTED, flightPlanRepo.store.get(flightPlanId).getStatus());
    }

    @Test
    public void shouldFailWhenPhysicsTestFails() {
        dslValidator.errors = List.of();
        physicsTester.errors = List.of("FUEL_INSUFFICIENT");

        final FlightPlanTestResult result = controller.test(flightPlanId, pilotId);

        assertFalse(result.passed());
        assertEquals(FlightPlanStatus.REJECTED, result.newStatus());
        assertEquals(List.of("FUEL_INSUFFICIENT"), result.physicsErrors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectWhenPilotDoesNotOwnPlan() {
        controller.test(flightPlanId, PilotId.newId());
    }

    @Test
    public void listTestablePlansShouldReturnOnlyOwnedDraftPlans() {
        final FlightPlan other = copyPlanForPilot(PilotId.newId(), FlightPlanStatus.ACTIVE);
        flightPlanRepo.save(other);

        final var plans = controller.listTestablePlansForCurrentPilot();

        assertEquals(1, plans.size());
        assertEquals(flightPlanId, plans.get(0).getId());
    }

    @Test
    public void testForCurrentPilotShouldResolvePilotFromUsername() {
        dslValidator.errors = List.of();
        physicsTester.errors = List.of();

        final FlightPlanTestResult result = controller.testForCurrentPilot(flightPlanId);

        assertTrue(result.passed());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectTestWhenPlanNotTestable() {
        flightPlan.setStatus(FlightPlanStatus.TESTED);
        flightPlanRepo.save(flightPlan);
        controller.test(flightPlanId, pilotId);
    }

    private FlightPlan copyPlanForPilot(final PilotId owner, final FlightPlanStatus status) {
        final FlightPlan fp = new FlightPlan(
                FlightPlanId.newId(),
                flightPlan.getAircraft(),
                flightPlan.getFlightRoute(),
                new FlightPlanDesignator("FP002"),
                flightPlan.getFlightType(),
                flightPlan.getDepartureDateTime(),
                flightPlan.getLegs(),
                flightPlan.getWind());
        fp.setPilot(owner);
        fp.setStatus(status);
        return fp;
    }

    private static final class RecordingDslValidator implements FlightPlanDslValidationPort {
        List<String> errors = List.of();

        @Override
        public List<String> validate(final FlightPlan flightPlan) {
            return errors;
        }
    }

    private static final class RecordingPhysicsTester implements FlightPlanPhysicsTestPort {
        List<String> errors = List.of();

        @Override
        public List<String> run(final Path inputJson) {
            return errors;
        }
    }

    private static final class FakeAuth implements AuthorizationPort {
        boolean checkedPilot;
        String username = "pilot@test.com";

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            checkedPilot = true;
            if (roles.length != 1 || roles[0] != Roles.PILOT) {
                throw new SecurityException("Expected PILOT role");
            }
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    private static final class TestPilotRepo implements PilotRepository {
        private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);
        PilotId pilotId;

        @Override
        public Optional<Pilot> findByEmail(final Email email) {
            if ("pilot@test.com".equals(email.value())) {
                return Optional.of(new Pilot(
                        pilotId,
                        new FullName("Test", "Pilot"),
                        email,
                        new CompanyId(UUID.randomUUID()),
                        new PilotCertification("ATPL"),
                        new License("LIC-1", FUTURE),
                        List.of(AircraftModelId.newId())));
            }
            return Optional.empty();
        }

        @Override
        public Optional<Pilot> ofIdentity(final String id) {
            return Optional.empty();
        }

        @Override
        public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
            return List.of();
        }

        @Override
        public Pilot save(final Pilot entity) {
            return entity;
        }

        @Override
        public Optional<Pilot> ofIdentity(final PilotId id) {
            return Optional.empty();
        }

        @Override
        public Iterable<Pilot> findAll() {
            return List.of();
        }

        @Override
        public void delete(final Pilot entity) {
        }

        @Override
        public void deleteOfIdentity(final PilotId id) {
        }

        @Override
        public long count() {
            return 0;
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
        public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
            return List.of();
        }

        @Override
        public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
            return false;
        }
    }
}
