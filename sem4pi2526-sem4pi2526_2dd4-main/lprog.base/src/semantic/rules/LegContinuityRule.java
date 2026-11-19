package semantic.rules;

import domain.FlightPlan;
import domain.Leg;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

public class LegContinuityRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        var legs = plan.flight().legs();
        for (int i = 0; i < legs.size() - 1; i++) {
            Leg current = legs.get(i);
            Leg next = legs.get(i + 1);
            if (!current.arrivalAirport().equals(next.departureAirport())) {
                errors.add(new SemanticError(
                        "LegContinuity",
                        next.line(),
                        "Departure airport of leg " + (i + 2) + " (" + next.departureAirport()
                                + ") does not match arrival airport of leg " + (i + 1)
                                + " (" + current.arrivalAirport() + ")"
                ));
            }
        }
        return errors;
    }
}