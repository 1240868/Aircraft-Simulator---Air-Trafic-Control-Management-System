package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class WeatherDataId implements ValueObject, Serializable, Comparable<WeatherDataId> {
    private static final long serialVersionUID = 1L;

    @Column(name = "weather_data_id", nullable = false, updatable = false)
    private String value;

    public WeatherDataId(final UUID value) {
        Objects.requireNonNull(value, "WeatherDataId is required");
        this.value = value.toString();
    }

    protected WeatherDataId() {
        // for ORM
    }

    public static WeatherDataId newId() {
        return new WeatherDataId(UUID.randomUUID());
    }

    public UUID asUuid() {
        return UUID.fromString(value);
    }

    @Override
    public int compareTo(final WeatherDataId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value;
    }
}

