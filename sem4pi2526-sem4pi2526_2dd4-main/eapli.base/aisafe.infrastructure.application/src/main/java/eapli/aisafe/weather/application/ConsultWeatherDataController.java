package eapli.aisafe.weather.application;

import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.weather.application.consultation.WeatherConsultationResult;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import java.time.LocalDate;

/** US043 — consult weather data for an air control area on a given UTC day. */
@UseCaseController
public class ConsultWeatherDataController {

    private final AuthorizationPort authz;
    private final ConsultWeatherDataService service;

    public ConsultWeatherDataController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    public ConsultWeatherDataController(final AuthorizationPort authz,
                                        final WeatherDataRepository weatherRepo,
                                        final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.service = new ConsultWeatherDataService(weatherRepo, areaRepo);
    }

    public ConsultWeatherDataController(final AuthorizationPort authz,
                                        final ConsultWeatherDataService service) {
        this.authz = authz;
        this.service = service;
    }

    public WeatherConsultationResult consult(final String areaCode, final LocalDate date) {
        authz.ensureAuthenticatedUserHasAnyOf(
                Roles.WEATHER_PERSON,
                Roles.PILOT,
                Roles.FLIGHT_CONTROL_OPERATOR);
        return service.consult(areaCode, date);
    }
}
