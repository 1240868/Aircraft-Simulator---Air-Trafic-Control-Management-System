package eapli.aisafe.reporting.application;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.domain.MonthlyReportStats;
import eapli.aisafe.reporting.domain.MonthlyStatisticsReport;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import java.nio.file.Path;
import java.time.YearMonth;

/** US112 — generate a monthly statistics report. Actor: Flight Control Operator. */
@UseCaseController
public class GenerateMonthlyReportController {

    private final AuthorizationPort authz;
    private final AirControlAreaRepository areaRepo;
    private final FlightPlanRepository flightPlanRepo;
    private final AirportRepository airportRepo;
    private final ReportExporter exporter;

    public GenerateMonthlyReportController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().airports(),
                new TextFileReportExporter());
    }

    public GenerateMonthlyReportController(final AuthorizationPort authz,
                                            final AirControlAreaRepository areaRepo,
                                            final FlightPlanRepository flightPlanRepo,
                                            final AirportRepository airportRepo,
                                            final ReportExporter exporter) {
        this.authz = authz;
        this.areaRepo = areaRepo;
        this.flightPlanRepo = flightPlanRepo;
        this.airportRepo = airportRepo;
        this.exporter = exporter;
    }

    public MonthlyReportResult generate(final int year, final int month) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.FLIGHT_CONTROL_OPERATOR);

        final String username = authz.currentUsername();
        final AirControlArea area = areaRepo.findByCollaboratorUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "No air control area found for user: " + username));

        final MonthlyReportDataCollector collector =
                new MonthlyReportDataCollector(flightPlanRepo, airportRepo, area.areaCode(), year, month);
        final MonthlyReportStats stats = collector.collect();

        final MonthlyStatisticsReport report = new MonthlyStatisticsReport(
                area.areaCode(), area.name(), YearMonth.of(year, month), stats);

        final Path path = exporter.export(report);
        return MonthlyReportResult.success(path);
    }
}
