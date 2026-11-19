package eapli.aisafe.simulation.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.roles.Roles;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.BeforeClass;
import org.junit.Test;

/** US111 — generate simulation report controller unit tests. */
public class GenerateSimulationReportControllerTest {

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new GenerateSimulationReportController());
    }

    @Test
    public void generateReportShouldEnforceFlightControlOperatorRole() {
        // Arrange
        final FakeAuth auth = new FakeAuth();
        final SimulationReportResult expected =
                SimulationReportResult.success("report", Paths.get("logs/simulation_report.txt"));
        final GenerateSimulationReportController ctrl =
                new GenerateSimulationReportController(auth, new StubService(expected));

        // Act
        ctrl.generateReport();

        // Assert
        assertNotNull(auth.lastRoles);
        assertEquals(1, auth.lastRoles.length);
        assertEquals(Roles.FLIGHT_CONTROL_OPERATOR, auth.lastRoles[0]);
    }

    @Test
    public void generateReportShouldDelegateToService() {
        // Arrange
        final SimulationReportResult expected =
                SimulationReportResult.success("report content", Paths.get("logs/simulation_report.txt"));
        final GenerateSimulationReportController ctrl =
                new GenerateSimulationReportController(new FakeAuth(), new StubService(expected));

        // Act
        final SimulationReportResult result = ctrl.generateReport();

        // Assert
        assertSame(expected, result);
    }

    @Test
    public void generateReportShouldPropagateServiceFailure() {
        // Arrange
        final SimulationReportResult failure =
                SimulationReportResult.failure("executable not found");
        final GenerateSimulationReportController ctrl =
                new GenerateSimulationReportController(new FakeAuth(), new StubService(failure));

        // Act
        final SimulationReportResult result = ctrl.generateReport();

        // Assert
        assertFalse(result.success());
        assertEquals("executable not found", result.errorMessage());
    }

    @Test
    public void successResultShouldHaveContentAndPath() {
        // Arrange
        final Path path = Paths.get("logs/simulation_report.txt");
        final String content = "report text";

        // Act
        final SimulationReportResult result = SimulationReportResult.success(content, path);

        // Assert
        assertTrue(result.success());
        assertEquals(content, result.reportContent());
        assertEquals(path, result.reportPath());
        assertFalse(result.errorMessage() != null);
    }

    @Test
    public void failureResultShouldCarryErrorMessage() {
        // Act
        final SimulationReportResult result = SimulationReportResult.failure("some error");

        // Assert
        assertFalse(result.success());
        assertEquals("some error", result.errorMessage());
    }

    // --- test doubles ---

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return "fco-test-user";
        }
    }

    private static final class StubService extends SimulationReportService {
        private final SimulationReportResult fixedResult;

        StubService(final SimulationReportResult fixedResult) {
            super(Path.of("."));
            this.fixedResult = fixedResult;
        }

        @Override
        public SimulationReportResult generateReport() {
            return fixedResult;
        }
    }
}
