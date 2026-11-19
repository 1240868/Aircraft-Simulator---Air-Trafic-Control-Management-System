package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WindSpeed implements ValueObject {
    @Column(name = "wind_speed_value", nullable = false)
    private double value;

    @Column(name = "wind_speed_unit", nullable = false, length = 20)
    private String unit;

    protected WindSpeed() {
    }

    public WindSpeed(final double value, final String unit) {
        Preconditions.ensure(value >= 0, "Wind speed must be non-negative");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
