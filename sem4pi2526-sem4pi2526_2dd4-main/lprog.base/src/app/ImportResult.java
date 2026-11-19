package app;

import domain.FlightPlan;
import parsing.SyntaxErrorListener.SyntaxError;
import semantic.SemanticError;

import java.util.List;

public final class ImportResult {

    private final FlightPlan flightPlan;
    private final List<SyntaxError> syntaxErrors;
    private final List<SemanticError> semanticErrors;

    private ImportResult(FlightPlan plan,
                         List<SyntaxError> syntaxErrors,
                         List<SemanticError> semanticErrors) {
        this.flightPlan = plan;
        this.syntaxErrors = List.copyOf(syntaxErrors);
        this.semanticErrors = List.copyOf(semanticErrors);
    }

    public static ImportResult ok(FlightPlan plan) {
        return new ImportResult(plan, List.of(), List.of());
    }

    public static ImportResult syntaxFailure(List<SyntaxError> errors) {
        return new ImportResult(null, errors, List.of());
    }

    public static ImportResult semanticFailure(List<SemanticError> errors) {
        return new ImportResult(null, List.of(), errors);
    }

    public boolean isValid() {
        return flightPlan != null && syntaxErrors.isEmpty() && semanticErrors.isEmpty();
    }

    public FlightPlan flightPlan() { return flightPlan; }
    public List<SyntaxError> syntaxErrors() { return syntaxErrors; }
    public List<SemanticError> semanticErrors() { return semanticErrors; }
}
