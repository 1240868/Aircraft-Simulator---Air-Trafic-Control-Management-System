package eapli.aisafe.weather.application;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import java.time.Instant;

@UseCaseController
public class RegisterWeatherDataController {

    private final AuthorizationPort authz;
    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;

    public RegisterWeatherDataController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    public RegisterWeatherDataController(final AuthorizationPort authz,
                                         final WeatherDataRepository weatherRepo,
                                         final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.weatherRepo = weatherRepo;
        this.areaRepo = areaRepo;
    }

    public WeatherData register(final String airControlAreaCode,
                                final Instant observedAt,
                                final double temperatureC,
                                final double humidityPercent,
                                final double windSpeedMs,
                                final double windDirectionDegrees) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.WEATHER_PERSON);

        final var code = AirControlAreaCode.valueOf(airControlAreaCode);
        if (areaRepo.findByCode(code).isEmpty()) {
            throw new IllegalArgumentException("Unknown air control area: " + code);
        }

        final var weatherData = new WeatherData(
                code,
                observedAt,
                windSpeedMs,
                windDirectionDegrees,
                temperatureC,
                humidityPercent
        );
        return weatherRepo.save(weatherData);
    }
}
