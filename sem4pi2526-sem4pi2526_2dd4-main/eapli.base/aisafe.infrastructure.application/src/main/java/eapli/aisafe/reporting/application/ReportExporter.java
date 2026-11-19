package eapli.aisafe.reporting.application;

import eapli.aisafe.reporting.domain.Report;
import java.nio.file.Path;

public interface ReportExporter {

    Path export(Report report);
}
