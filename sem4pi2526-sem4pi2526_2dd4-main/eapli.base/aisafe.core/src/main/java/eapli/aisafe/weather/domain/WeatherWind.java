package eapli.aisafe.weather.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class WeatherWind implements ValueObject {
    private WindSpeed speed;
    private WindDirection direction;
    private MeasurementUnit unit;

    protected WeatherWind() {
    }

    public WeatherWind(final WindSpeed speed, final WindDirection direction, final MeasurementUnit unit) {
        this.speed = speed;
        this.direction = direction;
        this.unit = unit;
    }

    public WindSpeed speed() { return speed; }
    public WindDirection direction() { return direction; }
    public MeasurementUnit unit() { return unit; }
}
