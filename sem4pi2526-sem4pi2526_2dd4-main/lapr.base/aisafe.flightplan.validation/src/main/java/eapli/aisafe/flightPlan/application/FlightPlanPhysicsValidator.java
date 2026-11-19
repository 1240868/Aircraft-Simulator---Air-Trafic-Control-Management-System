package eapli.aisafe.flightPlan.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * US085 — runs the external flight plan physics validator executable
 * (same pattern as {@link eapli.aisafe.simulation.application.SimulationReportService}).
 */
public final class FlightPlanPhysicsValidator implements FlightPlanPhysicsTestPort {

    static final String EXECUTABLE = "./flight_plan_validator";

    private final Path validatorBaseDir;

    public FlightPlanPhysicsValidator(final Path validatorBaseDir) {
        this.validatorBaseDir = validatorBaseDir;
    }

    @Override
    public List<String> run(final Path inputJson) {
        final List<String> errors = new ArrayList<>();

        if (!validatorBaseDir.toFile().isDirectory()) {
            errors.add("Validator directory not found: " + validatorBaseDir.toAbsolutePath());
            return errors;
        }

        final Path executable = validatorBaseDir.resolve("flight_plan_validator");
        if (!executable.toFile().canExecute()) {
            errors.add("Validator not found or not executable: "
                    + executable.toAbsolutePath()
                    + ". Compile: cd lapr.base && make compile");
            return errors;
        }

        if (inputJson == null || !inputJson.toFile().isFile()) {
            errors.add("Test payload file not found: " + inputJson);
            return errors;
        }

        try {
            final ProcessBuilder pb = new ProcessBuilder(EXECUTABLE, inputJson.toString());
            pb.directory(validatorBaseDir.toFile());
            pb.redirectErrorStream(true);

            final Process process = pb.start();
            boolean passed = false;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmed = line.trim();
                    if ("PASS".equals(trimmed)) {
                        passed = true;
                    } else if (trimmed.startsWith("FAIL|")) {
                        errors.add(trimmed.substring(5));
                    } else if (!trimmed.isEmpty()) {
                        errors.add(trimmed);
                    }
                }
            }

            final int exitCode = process.waitFor();
            if (exitCode == 2) {
                errors.add("Validator input error (exit code 2)");
            } else if (exitCode != 0 && passed) {
                errors.add("Validator exited with code " + exitCode);
            } else if (exitCode != 0 && errors.isEmpty()) {
                errors.add("Flight plan physics test failed (exit code " + exitCode + ")");
            }

            return errors;

        } catch (final IOException ex) {
            errors.add("I/O error running validator: " + ex.getMessage());
            return errors;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            errors.add("Validator was interrupted");
            return errors;
        }
    }
}
