package eapli.aisafe.weather.application.importing;

/** Per-row rejection detail for bulk weather import (US042). */
public record WeatherImportRowError(int lineNumber, String message) {
}
