package eapli.aisafe.weather.domain;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
public class WeatherData implements AggregateRoot<WeatherDataId> {
    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private WeatherDataId id;

    @Embedded
    private AirControlAreaCode areaCode;

    private Instant observedAt;

    private double windSpeedMs;
    private double windDirectionDegrees;
    private double temperatureC;
    private double humidityPercent;

    @Embedded
    private WeatherSource source;

    public WeatherData(final AirControlAreaCode areaCode,
                       final Instant observedAt,
                       final double windSpeedMs,
                       final double windDirectionDegrees,
                       final double temperatureC,
                       final double humidityPercent) {
        this(areaCode, observedAt, windSpeedMs, windDirectionDegrees, temperatureC, humidityPercent,
                WeatherSource.manualEntry());
    }

    public WeatherData(final AirControlAreaCode areaCode,
                       final Instant observedAt,
                       final double windSpeedMs,
                       final double windDirectionDegrees,
                       final double temperatureC,
                       final double humidityPercent,
                       final WeatherSource source) {
        Objects.requireNonNull(areaCode, "Area code is required");
        Objects.requireNonNull(observedAt, "ObservedAt is required");
        if (windSpeedMs < 0) throw new IllegalArgumentException("Wind speed must be >= 0");
        if (windDirectionDegrees < 0 || windDirectionDegrees >= 360) {
            throw new IllegalArgumentException("Wind direction must be in [0, 360[ degrees");
        }
        if (humidityPercent < 0 || humidityPercent > 100) {
            throw new IllegalArgumentException("Humidity must be in [0, 100] percent");
        }

        this.id = WeatherDataId.newId();
        this.areaCode = areaCode;
        this.observedAt = observedAt;
        this.windSpeedMs = windSpeedMs;
        this.windDirectionDegrees = windDirectionDegrees;
        this.temperatureC = temperatureC;
        this.humidityPercent = humidityPercent;
        this.source = source != null ? source : WeatherSource.csvDefault();
    }

    protected WeatherData() {
        // for ORM
    }

    public AirControlAreaCode areaCode() {
        return areaCode;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public double windSpeedMs() {
        return windSpeedMs;
    }

    public double windDirectionDegrees() {
        return windDirectionDegrees;
    }

    public double temperatureC() {
        return temperatureC;
    }

    public double humidityPercent() {
        return humidityPercent;
    }

    public WeatherSource source() {
        return source;
    }

    @Override
    public WeatherDataId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }
}

