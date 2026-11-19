package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds one demo aircraft for TAP so ATCC flows can be exercised (US071).
 */
public class FleetAircraftBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(FleetAircraftBootstrapper.class);

    @Override
    public boolean execute() {
        final var companies = PersistenceContext.repositories().airTransportCompanies();
        final var models = PersistenceContext.repositories().aircraftModels();
        final var aircraftRepo = PersistenceContext.repositories().aircraft();

        final var tap = companies.findByIataCode("TP");
        final var model = models.findByModelNameAndManufacturer("B737-800", "Boeing");
        if (tap.isEmpty() || model.isEmpty()) {
            LOGGER.warn("Skipping fleet aircraft bootstrap (missing TAP company or B737-800 model)");
            return true;
        }

        try {
            final var aircraft = AircraftFactory.registerNew(
                    "CS-TST",
                    "PT",
                    model.get().identity(),
                    tap.get().identity(),
                    List.of(new CabinSeatBlock("ECONOMY", 162), new CabinSeatBlock("BUSINESS", 18)),
                    180,
                    LocalDate.now().minusYears(8),
                    null);
            aircraftRepo.save(aircraft);
            LOGGER.debug("Bootstrapped aircraft {}", aircraft.identity());
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming demo aircraft already exists: {}", ex.getMessage());
        } catch (final IllegalArgumentException ex) {
            LOGGER.warn("Fleet aircraft bootstrap skipped: {}", ex.getMessage());
        }
        return true;
    }
}
