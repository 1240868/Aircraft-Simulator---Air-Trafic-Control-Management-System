package eapli.aisafe.simulation.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * US111 — runs the SCOMP C simulation executable and reads the generated report file.
 *
 * The C program (flight_sim) must be pre-compiled in the scomp.base directory
 * (cd scomp.base && make compile). It writes its report to
 * logs/simulation_report.txt inside that same directory.
 *
 * Stdout from the simulation is forwarded to the console in real time so the
 * operator can follow progress while waiting.
 */
public class SimulationReportService {

    static final String EXECUTABLE = "./flight_sim";
    static final String REPORT_RELATIVE_PATH = "logs/simulation_report.txt";

    private final Path scompBaseDir;

    public SimulationReportService(final Path scompBaseDir) {
        this.scompBaseDir = scompBaseDir;
    }

    /** Runs the C simulation and returns the generated report. */
    public SimulationReportResult generateReport() {
        if (!scompBaseDir.toFile().isDirectory()) {
            return SimulationReportResult.failure(
                    "Simulation directory not found: " + scompBaseDir.toAbsolutePath());
        }

        final Path executable = scompBaseDir.resolve("flight_sim");
        if (!executable.toFile().canExecute()) {
            return SimulationReportResult.failure(
                    "Simulation executable not found or not executable: "
                    + executable.toAbsolutePath()
                    + ". Compile first: cd scomp.base && make compile");
        }

        try {
            final ProcessBuilder pb = new ProcessBuilder(EXECUTABLE);
            pb.directory(scompBaseDir.toFile());
            pb.redirectErrorStream(true);

            final Process process = pb.start();

            // Stream C stdout to the console while the simulation runs
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[SIM] " + line);
                }
            }

            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                return SimulationReportResult.failure(
                        "Simulation process exited with code " + exitCode);
            }

            final Path reportPath = scompBaseDir.resolve(REPORT_RELATIVE_PATH);
            if (!reportPath.toFile().exists()) {
                return SimulationReportResult.failure(
                        "Report file not found after simulation: " + reportPath.toAbsolutePath());
            }

            final String content = Files.readString(reportPath);
            return SimulationReportResult.success(content, reportPath);

        } catch (final IOException ex) {
            return SimulationReportResult.failure("I/O error running simulation: " + ex.getMessage());
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SimulationReportResult.failure("Simulation was interrupted.");
        }
    }
}
