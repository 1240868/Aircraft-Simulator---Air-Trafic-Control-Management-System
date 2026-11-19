package eapli.aisafe.flightroute.repositories;

import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface FlightRouteRepository extends DomainRepository<FlightRouteId, FlightRoute> {
    Optional<FlightRoute> findById(String id);

    boolean existsByName(FlightRouteName name);
}
