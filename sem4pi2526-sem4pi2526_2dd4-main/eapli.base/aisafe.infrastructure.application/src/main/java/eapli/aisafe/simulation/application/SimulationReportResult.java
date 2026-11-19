package eapli.aisafe.simulation.application;

import java.nio.file.Path;

/** Outcome of a simulation run and report generation (US111). */
public record SimulationReportResult(
        boolean success,
        String reportContent,
        Path reportPath,
        String errorMessage) {

    public static SimulationReportResult success(final String content, final Path path) {
        return new SimulationReportResult(true, content, path, null);
    }

    public static SimulationReportResult failure(final String error) {
        return new SimulationReportResult(false, null, null, error);
    }
}
