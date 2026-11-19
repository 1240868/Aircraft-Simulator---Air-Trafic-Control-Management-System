package eapli.aisafe.reporting.application;

import java.nio.file.Path;

public record MonthlyReportResult(boolean success, Path reportPath, String errorMessage) {

    public static MonthlyReportResult success(final Path path) {
        return new MonthlyReportResult(true, path, null);
    }

    public static MonthlyReportResult failure(final String error) {
        return new MonthlyReportResult(false, null, error);
    }
}
