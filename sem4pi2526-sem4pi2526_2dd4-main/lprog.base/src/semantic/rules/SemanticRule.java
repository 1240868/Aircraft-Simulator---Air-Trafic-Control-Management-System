package semantic.rules;

import domain.FlightPlan;
import semantic.SemanticError;

import java.util.List;

public interface SemanticRule {
    List<SemanticError> validate(FlightPlan plan);
}
