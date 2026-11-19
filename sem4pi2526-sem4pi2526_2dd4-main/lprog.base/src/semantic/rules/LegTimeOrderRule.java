package semantic.rules;

import domain.FlightPlan;
import domain.Leg;
import semantic.SemanticError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LegTimeOrderRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        var legs = plan.flight().legs();

        // Rule 1: arrival must be after departure within the same leg
        for (Leg leg : legs) {
            if (leg.departureDate() == null || leg.departureTime() == null
                    || leg.arrivalDate() == null || leg.arrivalTime() == null) {
                continue;
            }
            LocalDateTime dep = LocalDateTime.of(leg.departureDate(), leg.departureTime());
            LocalDateTime arr = LocalDateTime.of(leg.arrivalDate(), leg.arrivalTime());
            if (!arr.isAfter(dep)) {
                errors.add(new SemanticError(
                        "LegTimeOrder",
                        leg.line(),
                        "Arrival (" + arr + ") must be strictly after departure (" + dep + ")"
                ));
            }
        }

        // Rule 2: arrival of leg n must precede departure of leg n+1
        for (int i = 0; i < legs.size() - 1; i++) {
            Leg current = legs.get(i);
            Leg next = legs.get(i + 1);

            if (current.arrivalDate() == null || current.arrivalTime() == null
                    || next.departureDate() == null || next.departureTime() == null) {
                continue;
            }

            LocalDateTime currentArr = LocalDateTime.of(current.arrivalDate(), current.arrivalTime());
            LocalDateTime nextDep = LocalDateTime.of(next.departureDate(), next.departureTime());

            if (!nextDep.isAfter(currentArr)) {
                errors.add(new SemanticError(
                        "LegTimeOrder",
                        next.line(),
                        "Departure of leg " + (i + 2) + " (" + nextDep
                                + ") must be after arrival of leg " + (i + 1) + " (" + currentArr + ")"
                ));
            }
        }

        return errors;
    }
}