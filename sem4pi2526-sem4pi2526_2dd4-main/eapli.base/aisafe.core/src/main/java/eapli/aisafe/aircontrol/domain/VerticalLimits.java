package eapli.aisafe.aircontrol.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class VerticalLimits implements ValueObject {
    private double minAltitude;
    private double maxAltitude;
    private String unit;

    protected VerticalLimits() {
    }

    public VerticalLimits(final double minAltitude, final double maxAltitude, final String unit) {
        Preconditions.ensure(minAltitude <= maxAltitude, "Min altitude must be <= max altitude");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.unit = unit;
    }

    public double minAltitude() { return minAltitude; }
    public double maxAltitude() { return maxAltitude; }
    public String unit() { return unit; }
}
