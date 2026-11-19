package eapli.aisafe.reporting.application;

import eapli.aisafe.reporting.domain.Report;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TextFileReportExporter implements ReportExporter {

    private static final DateTimeFormatter FILENAME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path REPORTS_DIR = Paths.get("reports");

    @Override
    public Path export(final Report report) {
        try {
            Files.createDirectories(REPORTS_DIR);
            final String filename = "monthly_report_"
                    + LocalDateTime.now().format(FILENAME_FMT) + ".txt";
            final Path path = REPORTS_DIR.resolve(filename);
            Files.writeString(path, report.render());
            return path;
        } catch (final IOException e) {
            throw new IllegalStateException(
                    "Could not write report file: " + e.getMessage(), e);
        }
    }
}
