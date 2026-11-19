package eapli.aisafe.weather.application.consultation;

import java.time.Instant;

/** Read model for one weather observation in a consultation (US043). */
public record WeatherDataSummary(
        String weatherDataId,
        String areaCode,
        Instant observedAt,
        double temperatureC,
        double humidityPercent,
        double windSpeedMs,
        double windDirectionDegrees,
        String source
) {
}
