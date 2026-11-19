package eapli.aisafe.weather.application.importing;

import java.util.List;

/** Summary of a bulk weather data import execution (US042). */
public record WeatherImportResult(
        int totalRows,
        int importedCount,
        int rejectedCount,
        List<WeatherImportRowError> errors
) {
}
