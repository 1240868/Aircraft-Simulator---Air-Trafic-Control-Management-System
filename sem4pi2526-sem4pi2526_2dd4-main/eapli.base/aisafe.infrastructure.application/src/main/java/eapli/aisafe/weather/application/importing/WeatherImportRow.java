package eapli.aisafe.weather.application.importing;

import java.time.Instant;

/** Transient representation of one parsed import file row (US042). */
public record WeatherImportRow(
        int lineNumber,
        String areaCode,
        Instant observedAt,
        double temperatureC,
        double humidityPercent,
        double windSpeedMs,
        double windDirectionDegrees,
        String source
) {
}
