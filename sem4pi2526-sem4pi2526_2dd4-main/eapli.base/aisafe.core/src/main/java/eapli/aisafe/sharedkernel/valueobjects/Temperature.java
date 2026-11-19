package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class Temperature implements ValueObject {
    private double value;
    private String unit;

    protected Temperature() {
    }

    public Temperature(final double value, final String unit) {
        Preconditions.nonEmpty(unit, "Unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
