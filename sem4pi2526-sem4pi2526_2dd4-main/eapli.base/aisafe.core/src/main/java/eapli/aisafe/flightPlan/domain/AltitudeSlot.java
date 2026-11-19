package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class AltitudeSlot implements ValueObject {
    @Column(name = "min_altitude", nullable = false)
    private double minAltitude;

    @Column(name = "max_altitude", nullable = false)
    private double maxAltitude;

    @Column(name = "altitude_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementUnit unit;

    protected AltitudeSlot() {
    }

    public AltitudeSlot(final double minAltitude, final double maxAltitude, final MeasurementUnit unit) {
        Preconditions.ensure(minAltitude <= maxAltitude, "Min altitude must be <= max altitude");
        Preconditions.ensure(minAltitude >= 0, "Min altitude must be >= 0");
        Preconditions.ensure(maxAltitude >= 0, "Max altitude must be >= 0");
        Preconditions.ensure(unit != null, "Unit must not be null");
        Preconditions.ensure(unit == MeasurementUnit.METER || unit == MeasurementUnit.FEET, "Unit must be meters or feet");
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.unit = unit;
    }

    public double minAltitude() { return minAltitude; }
    public double maxAltitude() { return maxAltitude; }
    public MeasurementUnit unit() { return unit; }
}
