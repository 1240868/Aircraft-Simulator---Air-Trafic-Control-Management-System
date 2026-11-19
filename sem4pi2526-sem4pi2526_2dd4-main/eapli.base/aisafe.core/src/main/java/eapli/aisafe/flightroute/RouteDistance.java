package eapli.aisafe.flightroute;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class RouteDistance implements ValueObject {
    @Column(name = "distance_value", nullable = false)
    private double value;

    @Column(name = "distance_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementUnit unit;

    protected RouteDistance() {
    }

    public RouteDistance(final double value, final MeasurementUnit unit) {
        Preconditions.ensure(value >= 0, "Distance must be non-negative");
        Preconditions.ensure(unit == MeasurementUnit.METER || unit == MeasurementUnit.MILE, "Unit must be meters or miles");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public MeasurementUnit unit() { return unit; }
}
