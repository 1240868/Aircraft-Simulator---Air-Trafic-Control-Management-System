package semantic.rules;

import domain.FlightPlan;
import domain.Leg;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

// The lexer enforces the shape (YYYY-MM-DD, HH:MM) and the visitor parses with
// java.time, which already rejects invalid calendar values (e.g. 2026-02-30).
// This rule exists for completeness and to flag null dates/times if any sneak through.
public class ValidDateTimeRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        for (Leg leg : plan.flight().legs()) {
            if (leg.departureDate() == null || leg.departureTime() == null) {
                errors.add(new SemanticError("ValidDateTime", leg.line(),
                        "Departure date/time is missing or invalid"));
            }
            if (leg.arrivalDate() == null || leg.arrivalTime() == null) {
                errors.add(new SemanticError("ValidDateTime", leg.line(),
                        "Arrival date/time is missing or invalid"));
            }
        }
        return errors;
    }
}