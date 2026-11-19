package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.App;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.PendingFlightsChecker;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.persistence.impl.inmemory.InMemoryPendingFlightsChecker;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaRepositoryFactory implements RepositoryFactory {

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new AisafeUserRepository(autoTx);
    }

    @Override
    public UserRepository users() {
        return new AisafeUserRepository();
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new JpaAirControlAreaRepository();
    }

    @Override
    public AirportRepository airports() {
        return new JpaAirportRepository();
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new JpaAirTransportCompanyRepository();
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return new JpaAircraftModelRepository();
    }

    @Override
    public AircraftRepository aircraft() {
        return new JpaAircraftRepository();
    }

    @Override
    public EngineModelRepository engineModels() {
        return new JpaEngineModelRepository();
    }

    @Override
    public eapli.aisafe.weather.repositories.WeatherDataRepository weatherData() {
        return new JpaWeatherDataRepository();
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return new JpaFlightPlanRepository();
    }

    @Override
    public FlightRouteRepository flightRoutes() {
        return new JpaFlightRouteRepository();
    }

    @Override
    public PilotRepository pilots() {
        return new JpaPilotRepository();
    }

    @Override
    public PendingFlightsChecker pendingFlightsChecker() {
        return new InMemoryPendingFlightsChecker();
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(App.settings().getPersistenceUnitName(),
                App.settings().getExtendedPersistenceProperties());
    }
}
