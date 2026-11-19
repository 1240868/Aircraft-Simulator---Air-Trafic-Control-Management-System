package eapli.aisafe.flightPlan.application;

import java.time.Instant;

public record InsertWeatherDataDTO(
        String flightPlanId,
        String legId,
        String areaCode,
        Instant observedAt,
        double windSpeedMs,
        double windDirectionDeg,
        double temperatureC,
        double humidityPercent) {
}
