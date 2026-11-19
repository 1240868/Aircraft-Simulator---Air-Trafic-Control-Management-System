package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class Visibility implements ValueObject {
    private double value;
    private String unit;

    protected Visibility() {
    }

    public Visibility(final double value, final String unit) {
        Preconditions.ensure(value >= 0, "Visibility must be non-negative");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
