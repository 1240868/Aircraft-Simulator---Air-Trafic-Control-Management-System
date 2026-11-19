package eapli.aisafe.app.backoffice.console.presentation.fleet;

import eapli.aisafe.fleet.application.RegisterAircraftModelController;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegisterAircraftModelUI extends AbstractUI {

    private final RegisterAircraftModelController controller = new RegisterAircraftModelController();

    @Override
    protected boolean doShow() {
        System.out.println("--- Existing engine models (use their IDs below) ---");
        for (final var em : controller.listEngineModels()) {
            System.out.println(em.identity() + "  |  " + em.modelName() + " / " + em.manufacturer() + "  |  " + em.type());
        }
        System.out.println("---------------------------------------------------");

        final String modelName = Console.readLine("Aircraft model name");
        final String manufacturer = Console.readLine("Manufacturer");
        final String typeStr = Console.readLine("Aircraft type (PASSENGER, CARGO, MIXED)");
        final int numberOfEngines = Console.readInteger("Number of engines (positive integer)");
        final int passengerCapacity = Console.readInteger("Maximum passenger capacity for this model (positive integer)");
        final double rangeKm = Console.readDouble("Maximum range (km)");
        final double emptyWeightKg = Console.readDouble("Empty weight (kg)");
        final double mtowKg = Console.readDouble("Maximum take-off weight - MTOW (kg)");
        final double mzfwKg = Console.readDouble("Maximum zero fuel weight - MZFW (kg)");
        final double maxFuelLiters = Console.readDouble("Maximum fuel capacity (liters)");
        final double serviceCeilingM = Console.readDouble("Service ceiling (m)");
        final double cruiseSpeedMs = Console.readDouble("Cruise speed (m/s)");
        final double wingAreaM2 = Console.readDouble("Wing area (m2)");
        final double cd0 = Console.readDouble("Zero-lift drag coefficient Cd0 (positive, dimensionless)");
        final double cl = Console.readDouble("Design lift coefficient Cl (positive, dimensionless)");
        final String idsLine = Console.readLine("Certified engine model IDs (comma-separated UUIDs, at least one)");

        try {
            final AircraftType type = AircraftType.valueOf(typeStr.trim().toUpperCase());
            final List<EngineModelId> ids = parseEngineIds(idsLine);
            final var am = controller.register(modelName, manufacturer, type, numberOfEngines, passengerCapacity,
                    rangeKm, emptyWeightKg, mtowKg, mzfwKg, maxFuelLiters, serviceCeilingM, cruiseSpeedMs,
                    wingAreaM2, cd0, cl, ids);
            System.out.println("Aircraft model registered: " + am.modelName() + " / " + am.manufacturer() + " id=" + am.identity());
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final IllegalArgumentException ex) {
            System.out.println("Invalid value: " + ex.getMessage());
        }
        return false;
    }

    private static List<EngineModelId> parseEngineIds(final String line) {
        final List<EngineModelId> out = new ArrayList<>();
        for (final String part : line.split(",")) {
            final String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            out.add(new EngineModelId(UUID.fromString(s)));
        }
        return out;
    }

    @Override
    public String headline() {
        return "Register aircraft model";
    }
}
