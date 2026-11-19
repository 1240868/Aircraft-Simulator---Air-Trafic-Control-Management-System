package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class WeatherSource implements ValueObject {
    private String value;

    protected WeatherSource() {
    }

    public WeatherSource(final String value) {
        Preconditions.nonEmpty(value, "Weather source is required");
        this.value = value.trim();
    }

    public static WeatherSource csvDefault() {
        return new WeatherSource("CSV-IMPORT");
    }

    public static WeatherSource manualEntry() {
        return new WeatherSource("MANUAL");
    }

    public String value() { return value; }
}
