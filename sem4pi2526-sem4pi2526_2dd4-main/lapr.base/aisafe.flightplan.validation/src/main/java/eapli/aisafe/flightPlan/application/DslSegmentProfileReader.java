package eapli.aisafe.flightPlan.application;

import app.FlightPlanImporter;
import app.ImportResult;
import domain.AltitudeSlot;
import domain.Leg;
import domain.Segment;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** US085 — reads cruise altitudes from the DSL source linked to a flight plan. */
public final class DslSegmentProfileReader {

    private final FlightPlanImporter importer;

    public DslSegmentProfileReader() {
        this(new FlightPlanImporter());
    }

    DslSegmentProfileReader(final FlightPlanImporter importer) {
        this.importer = importer;
    }

    public List<Double> readSegmentAltitudesMeters(final FlightPlan plan) throws IOException {
        final Path dslPath = resolveDslPath(plan);
        final ImportResult result = importer.importFromFile(dslPath);
        if (!result.isValid()) {
            throw new IllegalStateException("DSL source is not valid: " + dslPath);
        }

        final List<Double> altitudes = new ArrayList<>();
        for (final Leg leg : result.flightPlan().flight().legs()) {
            for (final Segment segment : leg.segments()) {
                for (final AltitudeSlot slot : segment.altitudeSlots()) {
                    altitudes.add(toMeters(slot.altitude(), slot.altitudeUnit()));
                }
            }
        }
        if (altitudes.isEmpty()) {
            throw new IllegalStateException("No segment altitudes found in DSL: " + dslPath);
        }
        return altitudes;
    }

    private static Path resolveDslPath(final FlightPlan plan) {
        final String rawPath = plan.getDslSourcePath();
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Flight plan has no DSL source path");
        }
        final Path path = Path.of(rawPath.trim());
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("DSL source file not found: " + path);
        }
        return path;
    }

    static double toMeters(final double value, final String unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Altitude unit is required");
        }
        final String normalized = unit.trim().toLowerCase();
        return switch (normalized) {
            case "m" -> value;
            case "ft" -> value * 0.3048;
            default -> throw new IllegalArgumentException("Unsupported altitude unit: " + unit);
        };
    }
}
