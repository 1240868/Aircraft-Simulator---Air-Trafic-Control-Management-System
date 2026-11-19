package semantic.rules;

import domain.AirportCode;
import domain.FlightPlan;
import domain.Leg;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoAirportRevisitRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        var legs = plan.flight().legs();
        if (legs.isEmpty()) return errors;

        Set<AirportCode> visited = new HashSet<>();
        // Sequence of airports: dep(1), arr(1)=dep(2), arr(2)=dep(3), ..., arr(n)
        visited.add(legs.get(0).departureAirport());

        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            AirportCode arr = leg.arrivalAirport();
            // The last arrival of one leg equals the next departure, so we only check arrivals.
            if (!visited.add(arr)) {
                errors.add(new SemanticError(
                        "NoAirportRevisit",
                        leg.line(),
                        "Airport " + arr + " is visited more than once in the flight"
                ));
            }
        }
        return errors;
    }
}