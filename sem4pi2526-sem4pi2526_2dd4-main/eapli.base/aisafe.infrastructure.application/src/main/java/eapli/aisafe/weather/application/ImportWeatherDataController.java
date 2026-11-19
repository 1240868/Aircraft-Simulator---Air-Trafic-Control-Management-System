package eapli.aisafe.weather.application;

import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.weather.application.importing.WeatherImportResult;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import java.nio.file.Path;
import java.util.List;

/** US042 — import bulk weather data from external files. */
@UseCaseController
public class ImportWeatherDataController {

    private final AuthorizationPort authz;
    private final ImportWeatherDataService service;

    public ImportWeatherDataController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    public ImportWeatherDataController(final AuthorizationPort authz,
                                       final WeatherDataRepository weatherRepo,
                                       final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.service = new ImportWeatherDataService(weatherRepo, areaRepo);
    }

    public ImportWeatherDataController(final AuthorizationPort authz,
                                       final ImportWeatherDataService service) {
        this.authz = authz;
        this.service = service;
    }

    public List<String> supportedFormats() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.WEATHER_PERSON);
        return service.supportedFormats();
    }

    public WeatherImportResult importFromFile(final String format, final Path filePath) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.WEATHER_PERSON);
        return service.importFromFile(format, filePath);
    }
}
