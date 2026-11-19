package eapli.aisafe.aircontrol.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class HorizontalLimits implements ValueObject {
    private double minLatitude;
    private double maxLatitude;
    private double minLongitude;
    private double maxLongitude;

    protected HorizontalLimits() {
    }

    public HorizontalLimits(final double minLatitude, final double maxLatitude,
                            final double minLongitude, final double maxLongitude) {
        Preconditions.ensure(minLatitude >= -90 && maxLatitude <= 90, "Invalid latitude range");
        Preconditions.ensure(minLongitude >= -180 && maxLongitude <= 180, "Invalid longitude range");
        Preconditions.ensure(minLatitude <= maxLatitude, "Min latitude must be <= max latitude");
        Preconditions.ensure(minLongitude <= maxLongitude, "Min longitude must be <= max longitude");
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    public double minLatitude() { return minLatitude; }
    public double maxLatitude() { return maxLatitude; }
    public double minLongitude() { return minLongitude; }
    public double maxLongitude() { return maxLongitude; }
}
