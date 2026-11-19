package eapli.aisafe.flightPlan.application;

import app.FlightPlanImporter;
import app.ImportResult;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import parsing.SyntaxErrorListener.SyntaxError;
import semantic.SemanticError;

/** US085 — validates a persisted flight plan against its DSL source file. */
public final class FlightPlanDslFileValidator implements FlightPlanDslValidationPort {

    private final FlightPlanImporter importer;

    public FlightPlanDslFileValidator() {
        this(new FlightPlanImporter());
    }

    FlightPlanDslFileValidator(final FlightPlanImporter importer) {
        this.importer = importer;
    }

    @Override
    public List<String> validate(final FlightPlan flightPlan) {
        final List<String> errors = new ArrayList<>();

        final Path dslPath = resolveDslPath(flightPlan);
        if (dslPath == null) {
            if (flightPlan.getDslSourcePath() == null || flightPlan.getDslSourcePath().isBlank()) {
                errors.add("No DSL source associated with flight plan (import from file or set dslSourcePath)");
            } else {
                errors.add("DSL source file not found: " + flightPlan.getDslSourcePath());
            }
            return errors;
        }

        try {
            final ImportResult result = importer.importFromFile(dslPath);
            if (result.isValid()) {
                return List.of();
            }
            for (final SyntaxError err : result.syntaxErrors()) {
                errors.add(err.toString());
            }
            for (final SemanticError err : result.semanticErrors()) {
                errors.add(err.toString());
            }
            if (errors.isEmpty()) {
                errors.add("DSL validation failed for file: " + dslPath);
            }
            return errors;
        } catch (final IOException ex) {
            errors.add("Cannot read DSL file: " + ex.getMessage());
            return errors;
        }
    }

    private static Path resolveDslPath(final FlightPlan flightPlan) {
        final String rawPath = flightPlan.getDslSourcePath();
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        final Path path = Path.of(rawPath.trim());
        return Files.isRegularFile(path) ? path : null;
    }
}
