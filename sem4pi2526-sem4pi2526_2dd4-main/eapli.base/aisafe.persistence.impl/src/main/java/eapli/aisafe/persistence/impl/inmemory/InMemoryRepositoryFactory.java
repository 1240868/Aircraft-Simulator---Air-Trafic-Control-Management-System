package eapli.aisafe.persistence.impl.inmemory;

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
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class InMemoryRepositoryFactory implements RepositoryFactory {

    private final UserRepository users = new InMemoryUserRepository();
    private final AirControlAreaRepository airControlAreas = new InMemoryAirControlAreaRepository();
    private final AirportRepository airports = new InMemoryAirportRepository();
    private final AirTransportCompanyRepository airTransportCompanies = new InMemoryAirTransportCompanyRepository();
    private final AircraftModelRepository aircraftModels = new InMemoryAircraftModelRepository();
    private final AircraftRepository aircraft = new InMemoryAircraftRepository();
    private final EngineModelRepository engineModels = new InMemoryEngineModelRepository();
    private final WeatherDataRepository weatherData = new InMemoryWeatherDataRepository();
    private final FlightPlanRepository flightPlans = new InMemoryFlightPlanRepository();
    private final FlightRouteRepository flightRoutes = new InMemoryFlightRouteRepository();
    private final PilotRepository pilots = new InMemoryPilotRepository();
    private final PendingFlightsChecker pendingFlightsChecker = new InMemoryPendingFlightsChecker();

    @Override
    public UserRepository users(final TransactionalContext tx) {
        return users;
    }

    @Override
    public UserRepository users() {
        return users(null);
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return airControlAreas;
    }

    @Override
    public AirportRepository airports() {
        return airports;
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return airTransportCompanies;
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return aircraftModels;
    }

    @Override
    public AircraftRepository aircraft() {
        return aircraft;
    }

    @Override
    public EngineModelRepository engineModels() {
        return engineModels;
    }

    @Override
    public WeatherDataRepository weatherData() {
        return weatherData;
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return flightPlans;
    }

    @Override
    public FlightRouteRepository flightRoutes() {
        return flightRoutes;
    }

    @Override
    public PilotRepository pilots() {
        return pilots;
    }

    @Override
    public PendingFlightsChecker pendingFlightsChecker() {
        return pendingFlightsChecker;
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return null;
    }
}
