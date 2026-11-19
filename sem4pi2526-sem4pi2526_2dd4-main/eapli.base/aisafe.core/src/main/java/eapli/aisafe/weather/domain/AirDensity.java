package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class AirDensity implements ValueObject {
    private double value;
    private String unit;

    protected AirDensity() {
    }

    public AirDensity(final double value, final String unit) {
        Preconditions.ensure(value > 0, "Air density must be positive");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
