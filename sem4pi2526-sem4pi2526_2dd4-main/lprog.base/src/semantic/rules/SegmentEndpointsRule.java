package semantic.rules;

import domain.FlightPlan;
import domain.Leg;
import domain.Segment;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

public class SegmentEndpointsRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        for (Leg leg : plan.flight().legs()) {
            for (Segment seg : leg.segments()) {
                if (seg.start().equals(seg.end())) {
                    errors.add(new SemanticError(
                            "SegmentEndpoints",
                            seg.line(),
                            "Segment start and end coordinates must be different"
                    ));
                }
            }
        }
        return errors;
    }
}