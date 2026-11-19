package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, FlightRouteId>
        implements FlightRouteRepository {

    public Optional<FlightRoute> findByName(final FlightRouteName name) {
        for (final FlightRoute route : findAll()) {
            if (route.getName().value().equals(name.value())) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByName(final FlightRouteName name) {
        return findByName(name).isPresent();
    }

    @Override
    public Optional<FlightRoute> findById(final String id) {
        for (final var r : super.findAll()) {
            if (r.identity().toString().equals(id)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
