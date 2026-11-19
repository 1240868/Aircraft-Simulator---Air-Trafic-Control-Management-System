package semantic.rules;

import domain.FlightPlan;
import domain.Leg;
import domain.Route;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteCoherenceRule implements SemanticRule {

    private static final Pattern IATA = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern ICAO = Pattern.compile("^[A-Z]{4}$");
    private static final Pattern AIRPORT_PREFIX_ROUTE = Pattern.compile("^([A-Z]{3,4})(\\d{3,})$");

    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        Route route = plan.flight().route();
        var legs = plan.flight().legs();

        if (route == null || legs.isEmpty()) {
            return errors;
        }

        String routeId = route.id();
        Leg first = legs.get(0);
        Leg last = legs.get(legs.size() - 1);

        if (routeId.contains("-")) {
            String[] parts = routeId.split("-", 2);
            checkOrigin(errors, parts[0], first);
            if (parts.length > 1) {
                checkDestination(errors, parts[1], last);
            }
            return errors;
        }

        parseAirportPrefixedRoute(routeId).ifPresent(endpoints -> {
            checkOrigin(errors, endpoints.origin(), first);
            if (endpoints.destination() != null) {
                checkDestination(errors, endpoints.destination(), last);
            }
        });

        return errors;
    }

    private record RouteEndpoints(String origin, String destination) {}

    private Optional<RouteEndpoints> parseAirportPrefixedRoute(String routeId) {
        Matcher matcher = AIRPORT_PREFIX_ROUTE.matcher(routeId);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String prefix = matcher.group(1);
        if (ICAO.matcher(prefix).matches()) {
            return Optional.of(new RouteEndpoints(prefix, null));
        }
        if (IATA.matcher(prefix).matches()) {
            return Optional.of(new RouteEndpoints(prefix, null));
        }
        return Optional.empty();
    }

    private void checkOrigin(List<SemanticError> errors, String origin, Leg first) {
        if (!first.departureAirport().code().equalsIgnoreCase(origin)) {
            errors.add(new SemanticError(
                    "RouteCoherence",
                    first.line(),
                    "Route origin (" + origin + ") does not match first leg departure ("
                            + first.departureAirport() + ")"
            ));
        }
    }

    private void checkDestination(List<SemanticError> errors, String destination, Leg last) {
        if (!last.arrivalAirport().code().equalsIgnoreCase(destination)) {
            errors.add(new SemanticError(
                    "RouteCoherence",
                    last.line(),
                    "Route destination (" + destination + ") does not match last leg arrival ("
                            + last.arrivalAirport() + ")"
            ));
        }
    }
}
