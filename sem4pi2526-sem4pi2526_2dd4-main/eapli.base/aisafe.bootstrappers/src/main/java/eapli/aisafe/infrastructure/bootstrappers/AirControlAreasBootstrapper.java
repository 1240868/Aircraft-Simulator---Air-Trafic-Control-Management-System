package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.aircontrol.application.RegisterAirControlAreaController;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirControlAreasBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirControlAreasBootstrapper.class);
    private final RegisterAirControlAreaController controller = new RegisterAirControlAreaController();

    @Override
    public boolean execute() {
        register("PT-LIS", "Lisboa FIR", 36.0, 43.0, -12.0, -6.0);
        return true;
    }

    private void register(final String code, final String name,
                           final double latMin, final double latMax,
                           final double lonMin, final double lonMax) {
        try {
            controller.registerAirControlArea(code, name, latMin, latMax, lonMin, lonMax);
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming air control area already exists: {} ({})", code, ex.getMessage());
        }
    }
}
