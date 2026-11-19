package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Thrust implements ValueObject {
    @Column(name = "thrust_value", nullable = false)
    private double value;

    @Column(name = "thrust_unit", nullable = false)
    private String unit;

    protected Thrust() {
    }

    public Thrust(final double value, final String unit) {
        Preconditions.ensure(value > 0, "Thrust value must be positive");
        Preconditions.nonEmpty(unit, "Thrust unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
