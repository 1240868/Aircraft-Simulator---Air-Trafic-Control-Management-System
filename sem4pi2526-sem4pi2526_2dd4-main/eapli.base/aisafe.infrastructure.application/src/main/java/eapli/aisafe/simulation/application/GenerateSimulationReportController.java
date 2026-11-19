package eapli.aisafe.simulation.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import java.nio.file.Path;
import java.nio.file.Paths;

/** US111 — generate a simulation report. Actor: Flight Control Operator. */
@UseCaseController
public class GenerateSimulationReportController {

    private static final Path DEFAULT_SCOMP_DIR = Paths.get("scomp.base");

    private final AuthorizationPort authz;
    private final SimulationReportService service;

    public GenerateSimulationReportController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                new SimulationReportService(DEFAULT_SCOMP_DIR));
    }

    public GenerateSimulationReportController(final AuthorizationPort authz,
                                               final SimulationReportService service) {
        this.authz = authz;
        this.service = service;
    }

    /**
     * Runs the SCOMP C simulation and returns the generated report.
     * Requires FLIGHT_CONTROL_OPERATOR role.
     */
    public SimulationReportResult generateReport() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.FLIGHT_CONTROL_OPERATOR);
        return service.generateReport();
    }
}
