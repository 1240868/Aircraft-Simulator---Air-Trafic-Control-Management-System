package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.company.application.RegisterAirTransportCompanyController;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirTransportCompaniesBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirTransportCompaniesBootstrapper.class);
    private final RegisterAirTransportCompanyController controller = new RegisterAirTransportCompanyController();

    @Override
    public boolean execute() {
        register("TAP Air Portugal", "TP", "TAP", "AOC-PT-001", LocalDate.now().plusYears(2));
        register("Ryanair", "FR", "RYR", "AOC-IE-100", LocalDate.now().plusYears(1));
        return true;
    }

    private void register(final String name, final String iata, final String icao,
                          final String certNumber, final LocalDate certExpiry) {
        try {
            controller.register(name, iata, icao, certNumber, certExpiry);
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming air transport company already exists: {} ({})", name, ex.getMessage());
        }
    }
}
