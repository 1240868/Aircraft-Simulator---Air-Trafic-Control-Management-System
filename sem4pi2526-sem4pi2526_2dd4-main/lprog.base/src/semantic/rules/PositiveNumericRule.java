package semantic.rules;

import domain.AltitudeSlot;
import domain.FlightPlan;
import domain.Leg;
import domain.Segment;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;

public class PositiveNumericRule implements SemanticRule {
    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();

        for (Leg leg : plan.flight().legs()) {
            if (leg.fuel() != null && leg.fuel().quantity() <= 0) {
                errors.add(new SemanticError(
                        "PositiveNumeric",
                        leg.fuel().line(),
                        "Fuel quantity must be strictly positive (got " + leg.fuel().quantity() + ")"
                ));
            }

            for (Segment seg : leg.segments()) {
                for (AltitudeSlot slot : seg.altitudeSlots()) {
                    if (slot.altitude() <= 0) {
                        errors.add(new SemanticError(
                                "PositiveNumeric",
                                slot.line(),
                                "Altitude must be strictly positive (got " + slot.altitude() + ")"
                        ));
                    }
                    if (slot.width() <= 0) {
                        errors.add(new SemanticError(
                                "PositiveNumeric",
                                slot.line(),
                                "Width must be strictly positive (got " + slot.width() + ")"
                        ));
                    }
                }
                if (seg.wind() != null && seg.wind().speed() < 0) {
                    errors.add(new SemanticError(
                            "PositiveNumeric",
                            seg.line(),
                            "Wind speed cannot be negative (got " + seg.wind().speed() + ")"
                    ));
                }
            }
        }
        return errors;
    }
}