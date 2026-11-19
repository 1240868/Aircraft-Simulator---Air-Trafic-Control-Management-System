package eapli.aisafe.weather.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;

@UseCaseController
public class InsertWeatherDataController {

    private final AuthorizationPort authz;
    private final WeatherDataRepository weatherRepo;
    private final FlightPlanRepository flightPlanRepo;

    public InsertWeatherDataController () {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().flightPlans());
    }
    public InsertWeatherDataController (final AuthorizationPort authz,
                                  final WeatherDataRepository weatherRepo,
                                  final FlightPlanRepository flightPlanRepo) {
        this.authz = authz;
        this.weatherRepo = weatherRepo;
        this.flightPlanRepo = flightPlanRepo;
    }

    public void insertWeatherData(FlightPlanId flightPlanId, WindSpeed speed, WindDirection direction) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);

        final var flightPlan = flightPlanRepo.findById(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown flight plan: " + flightPlanId));

        flightPlan.updateWeatherData(speed, direction);

        flightPlanRepo.save(flightPlan);
    }




}
