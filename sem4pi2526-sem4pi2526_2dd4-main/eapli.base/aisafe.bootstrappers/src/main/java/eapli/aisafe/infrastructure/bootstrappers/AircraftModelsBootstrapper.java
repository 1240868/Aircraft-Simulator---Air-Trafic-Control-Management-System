package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.fleet.application.RegisterAircraftModelController;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class AircraftModelsBootstrapper implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftModelsBootstrapper.class);

    private final RegisterAircraftModelController controller = new RegisterAircraftModelController();

    @Override
    public boolean execute() {
        final var engines = PersistenceContext.repositories().engineModels().findAll();
        final var firstEngine = engines.iterator().hasNext() ? engines.iterator().next() : null;
        if (firstEngine == null) {
            LOGGER.warn("No engine models found; skipping aircraft model bootstrap");
            return false;
        }

        // B737-800: emptyWeight=41140kg, MTOW=79016kg, MZFW=62731kg, fuel=26022L,
        //           ceiling=12500m, cruiseSpeed=233m/s, wingArea=125.08m2, Cd0=0.025, Cl=0.5
        register("B737-800", "Boeing", AircraftType.PASSENGER, 2, 189, 5765.0,
                41140.0, 79016.0, 62731.0, 26022.0, 12500.0, 233.0, 125.08, 0.025, 0.5,
                List.of(firstEngine.identity()));

        return true;
    }

    private void register(final String modelName,
                          final String manufacturer,
                          final AircraftType type,
                          final int numberOfEngines,
                          final int passengerCapacity,
                          final double maximumRangeKm,
                          final double emptyWeightKg,
                          final double mtowKg,
                          final double mzfwKg,
                          final double maxFuelCapacityLiters,
                          final double serviceCeilingM,
                          final double cruiseSpeedMs,
                          final double wingAreaM2,
                          final double cd0,
                          final double cl,
                          final List<EngineModelId> certifiedEngines) {
        try {
            controller.register(modelName, manufacturer, type, numberOfEngines, passengerCapacity, maximumRangeKm,
                    emptyWeightKg, mtowKg, mzfwKg, maxFuelCapacityLiters, serviceCeilingM, cruiseSpeedMs,
                    wingAreaM2, cd0, cl, certifiedEngines);
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming aircraft model already exists: {} / {}", modelName, manufacturer);
        }
    }
}

