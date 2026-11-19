package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class FlightWind implements ValueObject {
    @Embedded
    private WindSpeed speed;
    @Embedded
    private WindDirection direction;
    @Column(name = "wind_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementUnit unit;

    protected FlightWind() {
    }

    public FlightWind(final WindSpeed speed, final WindDirection direction, final MeasurementUnit unit) {
        this.speed = speed;
        this.direction = direction;
        this.unit = unit;
    }

    public WindSpeed speed() { return speed; }
    public WindDirection direction() { return direction; }
    public MeasurementUnit unit() { return unit; }
}
