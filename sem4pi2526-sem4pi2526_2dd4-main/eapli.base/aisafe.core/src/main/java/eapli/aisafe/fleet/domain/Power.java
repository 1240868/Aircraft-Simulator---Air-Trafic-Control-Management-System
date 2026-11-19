package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Power implements ValueObject {
    @Column(name = "power_value", nullable = true)
    private double value;

    @Column(name = "power_unit", nullable = true)
    private String unit;

    protected Power() {
    }

    public Power(final double value, final String unit) {
        Preconditions.ensure(value > 0, "Power value must be positive");
        Preconditions.nonEmpty(unit, "Power unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
