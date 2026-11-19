package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.fleet.application.RegisterEngineModelController;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import jakarta.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineModelsBootstrapper implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineModelsBootstrapper.class);

    private final RegisterEngineModelController controller = new RegisterEngineModelController();

    @Override
    public boolean execute() {
        registerJet("CFM56-7B", "CFM International", EngineType.TURBOFAN,
                121.0, "kN", 100.0, 0.60,
                0.804, 42.80);
        registerJet("Trent 1000", "Rolls-Royce", EngineType.TURBOFAN,
                290.0, "kN", 250.0, 0.62,
                0.804, 42.80);
        return true;
    }

    private void registerJet(final String modelName,
                             final String manufacturer,
                             final EngineType type,
                             final double seaLevelStaticThrust,
                             final String thrustUnit,
                             final double cruiseThrust,
                             final double tsfc,
                             final double fuelDensity,
                             final double fuelSpecificEnergy) {
        try {
            controller.registerJet(modelName, manufacturer, type,
                    seaLevelStaticThrust, thrustUnit, cruiseThrust, tsfc,
                    fuelDensity, fuelSpecificEnergy);
        } catch (final IntegrityViolationException | RollbackException ex) {
            LOGGER.warn("Assuming engine model already exists: {} / {}", modelName, manufacturer);
        }
    }
}

