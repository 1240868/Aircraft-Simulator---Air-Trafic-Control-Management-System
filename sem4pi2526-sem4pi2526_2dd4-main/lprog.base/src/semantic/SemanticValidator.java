package semantic;

import domain.FlightPlan;
import semantic.rules.AirportCodeFormatRule;
import semantic.rules.CoordinatesRangeRule;
import semantic.rules.LegContinuityRule;
import semantic.rules.LegTimeOrderRule;
import semantic.rules.NoAirportRevisitRule;
import semantic.rules.PositiveNumericRule;
import semantic.rules.RouteCoherenceRule;
import semantic.rules.SegmentEndpointsRule;
import semantic.rules.SemanticRule;
import semantic.rules.UniqueFlightIdRule;
import semantic.rules.ValidDateTimeRule;

import java.util.ArrayList;
import java.util.List;

public class SemanticValidator {
    private final List<SemanticRule> rules = List.of(
            new UniqueFlightIdRule(),
            new LegContinuityRule(),
            new LegTimeOrderRule(),
            new RouteCoherenceRule(),
            new NoAirportRevisitRule(),
            new SegmentEndpointsRule(),
            new PositiveNumericRule(),
            new CoordinatesRangeRule(),
            new AirportCodeFormatRule(),
            new ValidDateTimeRule()
    );

    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> all = new ArrayList<>();
        for (SemanticRule rule : rules) {
            all.addAll(rule.validate(plan));
        }
        return all;
    }
}
