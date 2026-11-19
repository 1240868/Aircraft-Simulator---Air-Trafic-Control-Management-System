package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WindDirection implements ValueObject {
    @Column(name = "wind_direction_degrees", nullable = false)
    private double degrees;

    protected WindDirection() {
    }

    public WindDirection(final double degrees) {
        Preconditions.ensure(degrees >= 0 && degrees < 360, "Wind direction must be between 0 and 360");
        this.degrees = degrees;
    }

    public double degrees() { return degrees; }
}
