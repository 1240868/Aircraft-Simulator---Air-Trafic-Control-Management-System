package semantic.rules;

import domain.Coordinates;
import domain.FlightPlan;
import domain.Leg;
import domain.Segment;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

public class CoordinatesRangeRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        for (Leg leg : plan.flight().legs()) {
            for (Segment seg : leg.segments()) {
                checkCoords(seg.start(), errors);
                checkCoords(seg.end(),   errors);
            }
        }
        return errors;
    }

    private void checkCoords(Coordinates c, List<SemanticError> errors) {
        if (c.latitude() < -90 || c.latitude() > 90) {
            errors.add(new SemanticError(
                    "CoordinatesRange",
                    c.line(),
                    "Latitude " + c.latitude() + " is out of range [-90, 90]"
            ));
        }
        if (c.longitude() < -180 || c.longitude() > 180) {
            errors.add(new SemanticError(
                    "CoordinatesRange",
                    c.line(),
                    "Longitude " + c.longitude() + " is out of range [-180, 180]"
            ));
        }
    }
}