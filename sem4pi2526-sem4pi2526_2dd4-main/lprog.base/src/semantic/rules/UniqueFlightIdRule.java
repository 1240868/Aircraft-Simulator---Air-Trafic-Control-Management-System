package semantic.rules;

import domain.FlightPlan;
import semantic.SemanticError;

import java.util.List;

// With a single flight per file this is trivially true; kept for completeness
// in case the DSL is extended to multiple flights per file.
public class UniqueFlightIdRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        return List.of();
    }
}