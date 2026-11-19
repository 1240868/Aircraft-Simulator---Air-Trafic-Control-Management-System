package eapli.aisafe.reporting.domain;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.framework.validations.Preconditions;
import java.time.YearMonth;
import java.util.List;

public final class MonthlyStatisticsReport extends Report {

    private final AirControlAreaCode areaCode;
    private final String areaName;
    private final YearMonth period;
    private final MonthlyReportStats stats;

    public MonthlyStatisticsReport(final AirControlAreaCode areaCode,
                                    final String areaName,
                                    final YearMonth period,
                                    final MonthlyReportStats stats) {
        super();
        Preconditions.nonNull(areaCode, "Area code is required");
        Preconditions.nonEmpty(areaName, "Area name is required");
        Preconditions.nonNull(period, "Period is required");
        Preconditions.nonNull(stats, "Stats are required");
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.period = period;
        this.stats = stats;
    }

    @Override
    public String title() {
        return "Monthly Statistics Report — " + areaName + " [" + areaCode + "] — " + period;
    }

    @Override
    protected List<ReportSection> sections() {
        return List.of(statusSection(), typeSection(), activitySection());
    }

    private ReportSection statusSection() {
        final String content = String.format(
                "  Total               : %d%n"
                + "  Approved            : %d%n"
                + "  Rejected            : %d%n"
                + "  Draft               : %d%n"
                + "  Submitted           : %d%n"
                + "  Completed           : %d%n"
                + "  Cancelled           : %d%n"
                + "  Active              : %d%n"
                + "  Pending Test        : %d%n"
                + "  Tested              : %d%n",
                stats.totalFlightPlans(),
                stats.approvedFlightPlans(),
                stats.rejectedFlightPlans(),
                stats.draftFlightPlans(),
                stats.submittedFlightPlans(),
                stats.completedFlightPlans(),
                stats.cancelledFlightPlans(),
                stats.activeFlightPlans(),
                stats.pendingTestFlightPlans(),
                stats.testedFlightPlans());
        return new ReportSection("Flight Plans by Status", content);
    }

    private ReportSection typeSection() {
        final String content = String.format(
                "  Commercial          : %d%n"
                + "  Cargo               : %d%n"
                + "  Charter             : %d%n"
                + "  Private             : %d%n"
                + "  Military            : %d%n"
                + "  Emergency           : %d%n",
                stats.commercialFlights(),
                stats.cargoFlights(),
                stats.charterFlights(),
                stats.privateFlights(),
                stats.militaryFlights(),
                stats.emergencyFlights());
        return new ReportSection("Flight Plans by Type", content);
    }

    private ReportSection activitySection() {
        final String content = String.format(
                "  Distinct pilots     : %d%n"
                + "  Distinct aircraft   : %d%n",
                stats.distinctPilots(),
                stats.distinctAircraft());
        return new ReportSection("Monthly Activity", content);
    }

    public MonthlyReportStats stats()          { return stats; }
    public YearMonth period()                  { return period; }
    public AirControlAreaCode areaCode()       { return areaCode; }
}
