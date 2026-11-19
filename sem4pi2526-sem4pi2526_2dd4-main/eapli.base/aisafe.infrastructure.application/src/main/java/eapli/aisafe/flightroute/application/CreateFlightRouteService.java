package eapli.aisafe.flightroute.application;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.FleetListing;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.aisafe.flightroute.HaversineRouteDistanceCalculator;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.IntegrityViolationException;

/** Coordinates flight route creation for an ATCC's company (US073). */
public class CreateFlightRouteService {

    private final FlightRouteRepository routeRepo;
    private final AirportRepository airportRepo;
    private final AirTransportCompanyRepository companyRepo;

    public CreateFlightRouteService(final FlightRouteRepository routeRepo,
                                    final AirportRepository airportRepo,
                                    final AirTransportCompanyRepository companyRepo) {
        this.routeRepo = routeRepo;
        this.airportRepo = airportRepo;
        this.companyRepo = companyRepo;
    }

    public FlightRouteDTO createRoute(final String numericSuffix,
                                      final String originIcao,
                                      final String destinationIcao,
                                      final String username) {
        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, username);
        final AirTransportCompany company = companyRepo.ofIdentity(companyId)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + companyId));

        final FlightRouteName routeName = FlightRouteName.compose(company.iataCode().value(), numericSuffix);
        if (routeRepo.existsByName(routeName)) {
            throw new IntegrityViolationException("Route name already exists: " + routeName.value());
        }

        final Airport origin = requireOperationalAirport(originIcao);
        final Airport destination = requireOperationalAirport(destinationIcao);

        final var distance = HaversineRouteDistanceCalculator.calculateDistance(origin, destination);
        final var duration = HaversineRouteDistanceCalculator.estimateDuration(distance);

        final FlightRoute route = FlightRoute.create(
                routeName,
                companyId,
                AirportId.forIcaoCode(origin.icaoCode()),
                AirportId.forIcaoCode(destination.icaoCode()),
                distance,
                duration);

        final FlightRoute saved = routeRepo.save(route);
        return toDto(saved, company, origin, destination);
    }

    private Airport requireOperationalAirport(final String icaoRaw) {
        final String icao = icaoRaw == null ? "" : icaoRaw.trim().toUpperCase();
        final Airport airport = airportRepo.findByIcaoCode(icao)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found: " + icaoRaw));
        if (!airport.isOperational()) {
            throw new IllegalArgumentException("Airport is not operational: " + icao);
        }
        return airport;
    }

    private static FlightRouteDTO toDto(final FlightRoute route,
                                        final AirTransportCompany company,
                                        final Airport origin,
                                        final Airport destination) {
        return new FlightRouteDTO(
                route.identity().toString(),
                route.getName().value(),
                company.name().value(),
                origin.icaoCode(),
                origin.iataCode(),
                destination.icaoCode(),
                destination.iataCode(),
                route.getDistance().value() / 1000.0,
                route.getDuration().minutes(),
                route.getStatus().name());
    }
}
