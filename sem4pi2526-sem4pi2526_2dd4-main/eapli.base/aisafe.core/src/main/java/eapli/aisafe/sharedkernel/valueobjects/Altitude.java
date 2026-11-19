package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Altitude implements ValueObject {
    @Column(name = "altitude_value", nullable = false)
    private double value;

    @Column(name = "altitude_unit", nullable = false, length = 20)
    private MeasurementUnit unit;

    protected Altitude() {
    }

    public Altitude(final double value, final MeasurementUnit unit) {
        Preconditions.areEqual(unit, MeasurementUnit.METER, "Only meters are supported for altitude");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public MeasurementUnit unit() { return unit; }
}
