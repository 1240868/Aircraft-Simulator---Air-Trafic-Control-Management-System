package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class JpaFlightRouteRepository
        extends BaseJpaRepositoryBase<FlightRoute, FlightRouteId, FlightRouteId>
        implements FlightRouteRepository {

    JpaFlightRouteRepository() {
        super("id");
    }

    public Optional<FlightRoute> findByName(final FlightRouteName name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name.value());
        for (final FlightRoute route : match("e.name.value = :name", params)) {
            return Optional.of(route);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByName(final FlightRouteName name) {
        return findByName(name).isPresent();
    }

    @Override
    public Optional<FlightRoute> findById(final String id) {
        return matchOne("e.id.value = :id", "id", id);
    }
}
