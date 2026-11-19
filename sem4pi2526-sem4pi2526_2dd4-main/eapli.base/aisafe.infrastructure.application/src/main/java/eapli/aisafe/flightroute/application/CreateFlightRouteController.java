package eapli.aisafe.flightroute.application;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** US073 — create a flight route for the authenticated collaborator's company. */
@UseCaseController
public class CreateFlightRouteController {

    private final AuthorizationPort authz;
    private final CreateFlightRouteService service;
    private final AirportRepository airportRepo;

    public CreateFlightRouteController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public CreateFlightRouteController(final AuthorizationPort authz,
                                       final FlightRouteRepository routeRepo,
                                       final AirportRepository airportRepo,
                                       final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.airportRepo = airportRepo;
        this.service = new CreateFlightRouteService(routeRepo, airportRepo, companyRepo);
    }

    public CreateFlightRouteController(final AuthorizationPort authz,
                                       final CreateFlightRouteService service,
                                       final AirportRepository airportRepo) {
        this.authz = authz;
        this.service = service;
        this.airportRepo = airportRepo;
    }

    public List<OperationalAirportDTO> listOperationalAirports() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final List<OperationalAirportDTO> out = new ArrayList<>();
        for (final Airport airport : airportRepo.findAll()) {
            if (airport.isOperational()) {
                out.add(new OperationalAirportDTO(
                        airport.icaoCode(),
                        airport.iataCode(),
                        airport.name(),
                        airport.town(),
                        airport.country()));
            }
        }
        out.sort(Comparator.comparing(OperationalAirportDTO::icaoCode));
        return out;
    }

    public FlightRouteDTO createRoute(final String numericSuffix,
                                      final String originIcao,
                                      final String destinationIcao) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        return service.createRoute(numericSuffix, originIcao, destinationIcao, authz.currentUsername());
    }
}
