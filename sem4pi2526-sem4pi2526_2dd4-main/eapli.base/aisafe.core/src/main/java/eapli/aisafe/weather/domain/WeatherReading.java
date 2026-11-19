package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class WeatherReading implements ValueObject {
    private LocalDateTime timestamp;
    private double temperature;
    private double pressure;
    private double humidity;

    protected WeatherReading() {
    }

    public WeatherReading(final LocalDateTime timestamp, final double temperature,
                          final double pressure, final double humidity) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
    }

    public LocalDateTime timestamp() { return timestamp; }
    public double temperature() { return temperature; }
    public double pressure() { return pressure; }
    public double humidity() { return humidity; }
}
