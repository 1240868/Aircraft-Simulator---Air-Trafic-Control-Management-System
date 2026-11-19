package eapli.aisafe.reporting.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.reporting.domain.Report;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/** US112 — GenerateMonthlyReportController unit tests. */
public class GenerateMonthlyReportControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new GenerateMonthlyReportController());
    }

    @Test
    public void generateShouldEnforceFlightControlOperatorRole() {
        // Arrange
        final FakeAuth auth = new FakeAuth("fco-user");
        final StubAreaRepo areaRepo = new StubAreaRepo(buildArea("fco-user"));
        final StubFlightPlanRepo fpRepo = new StubFlightPlanRepo(List.of());
        final CapturingExporter exporter = new CapturingExporter();
        final GenerateMonthlyReportController ctrl =
                new GenerateMonthlyReportController(auth, areaRepo, fpRepo, new StubAirportRepo(), exporter);

        // Act
        ctrl.generate(2026, 5);

        // Assert
        assertNotNull(auth.lastRoles);
        assertEquals(1, auth.lastRoles.length);
        assertEquals(Roles.FLIGHT_CONTROL_OPERATOR, auth.lastRoles[0]);
    }

    @Test
    public void generateShouldReturnSuccessWhenAreaFound() {
        // Arrange
        final FakeAuth auth = new FakeAuth("fco-user");
        final StubAreaRepo areaRepo = new StubAreaRepo(buildArea("fco-user"));
        final StubFlightPlanRepo fpRepo = new StubFlightPlanRepo(List.of());
        final CapturingExporter exporter = new CapturingExporter();
        final GenerateMonthlyReportController ctrl =
                new GenerateMonthlyReportController(auth, areaRepo, fpRepo, new StubAirportRepo(), exporter);

        // Act
        final MonthlyReportResult result = ctrl.generate(2026, 5);

        // Assert
        assertTrue(result.success());
        assertNotNull(result.reportPath());
    }

    @Test(expected = IllegalStateException.class)
    public void generateShouldThrowWhenNoAreaFoundForUser() {
        // Arrange
        final FakeAuth auth = new FakeAuth("unknown-user");
        final StubAreaRepo areaRepo = new StubAreaRepo(null);
        final StubFlightPlanRepo fpRepo = new StubFlightPlanRepo(List.of());
        final CapturingExporter exporter = new CapturingExporter();
        final GenerateMonthlyReportController ctrl =
                new GenerateMonthlyReportController(auth, areaRepo, fpRepo, new StubAirportRepo(), exporter);

        // Act
        ctrl.generate(2026, 5);
    }

    @Test
    public void generateShouldDelegateExportToExporter() {
        // Arrange
        final FakeAuth auth = new FakeAuth("fco-user");
        final StubAreaRepo areaRepo = new StubAreaRepo(buildArea("fco-user"));
        final StubFlightPlanRepo fpRepo = new StubFlightPlanRepo(List.of());
        final CapturingExporter exporter = new CapturingExporter();
        final GenerateMonthlyReportController ctrl =
                new GenerateMonthlyReportController(auth, areaRepo, fpRepo, new StubAirportRepo(), exporter);

        // Act
        ctrl.generate(2026, 5);

        // Assert
        assertNotNull(exporter.lastReport);
    }

    // --- helpers ---

    private static AirControlArea buildArea(final String collaboratorUsername) {
        final AirControlArea area = new AirControlArea("LPPO", "Porto FIR",
                35.0, 45.0, -10.0, 0.0);
        area.addCollaborator(collaboratorUsername);
        return area;
    }

    // --- test doubles ---

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;
        private final String username;

        FakeAuth(final String username) {
            this.username = username;
        }

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    private static final class StubAreaRepo
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
            implements AirControlAreaRepository {

        private final AirControlArea fixed;

        StubAreaRepo(final AirControlArea fixed) {
            this.fixed = fixed;
        }

        @Override
        public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
            return Optional.ofNullable(fixed);
        }

        @Override
        public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
            if (fixed != null && fixed.collaboratorUsernames().contains(username)) {
                return Optional.of(fixed);
            }
            return Optional.empty();
        }
    }

    private static final class StubFlightPlanRepo
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {

        private final List<FlightPlan> plans;

        StubFlightPlanRepo(final List<FlightPlan> plans) {
            this.plans = plans;
        }

        @Override
        public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
            return plans;
        }

        @Override
        public boolean hasAssignedFlightPlansFor(final eapli.aisafe.pilot.PilotId pilotId) {
            return false;
        }
    }

    private static final class StubAirportRepo
            extends InMemoryDomainRepository<Airport, String>
            implements AirportRepository {

        @Override
        public java.util.Optional<Airport> findByIcaoCode(final String icaoCode) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<Airport> findByIataCode(final String iataCode) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<Airport> findById(final String code) {
            return java.util.Optional.empty();
        }

        @Override
        public Boolean exists(final AirportId id) {
            return false;
        }

        @Override
        public Iterable<Airport> findByAirControlAreaCode(final AirControlAreaCode areaCode) {
            return List.of();
        }
    }

    private static final class CapturingExporter implements ReportExporter {
        Report lastReport;

        @Override
        public Path export(final Report report) {
            this.lastReport = report;
            return Paths.get("reports/monthly_report_test.txt");
        }
    }
}
