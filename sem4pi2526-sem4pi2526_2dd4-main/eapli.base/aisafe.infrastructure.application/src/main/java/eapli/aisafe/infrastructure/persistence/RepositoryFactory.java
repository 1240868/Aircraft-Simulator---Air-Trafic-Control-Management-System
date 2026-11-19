package eapli.aisafe.infrastructure.persistence;

import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.PendingFlightsChecker;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;

public interface RepositoryFactory {
    TransactionalContext newTransactionalContext();
    UserRepository users(TransactionalContext autoTx);
    UserRepository users();
    AirControlAreaRepository airControlAreas();
    AirportRepository airports();
    AirTransportCompanyRepository airTransportCompanies();
    AircraftModelRepository aircraftModels();
    AircraftRepository aircraft();
    EngineModelRepository engineModels();
    WeatherDataRepository weatherData();
    FlightPlanRepository flightPlans();
    FlightRouteRepository flightRoutes();
    PilotRepository pilots();
    PendingFlightsChecker pendingFlightsChecker();
}
