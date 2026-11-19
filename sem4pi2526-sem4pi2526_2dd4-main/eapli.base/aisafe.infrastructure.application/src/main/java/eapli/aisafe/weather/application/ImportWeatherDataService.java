package eapli.aisafe.weather.application;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.weather.application.importing.ImportParseException;
import eapli.aisafe.weather.application.importing.UnsupportedImportFormatException;
import eapli.aisafe.weather.application.importing.WeatherDataImportParser;
import eapli.aisafe.weather.application.importing.WeatherImportResult;
import eapli.aisafe.weather.application.importing.WeatherImportRow;
import eapli.aisafe.weather.application.importing.WeatherImportRowError;
import eapli.aisafe.weather.application.importing.CsvWeatherDataImportParser;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherSource;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordinates bulk weather data import (US042). */
public class ImportWeatherDataService {

    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;
    private final Map<String, WeatherDataImportParser> parsers;

    public ImportWeatherDataService(final WeatherDataRepository weatherRepo,
                                    final AirControlAreaRepository areaRepo) {
        this(weatherRepo, areaRepo, defaultParsers());
    }

    public ImportWeatherDataService(final WeatherDataRepository weatherRepo,
                                    final AirControlAreaRepository areaRepo,
                                    final Map<String, WeatherDataImportParser> parsers) {
        this.weatherRepo = weatherRepo;
        this.areaRepo = areaRepo;
        this.parsers = parsers;
    }

    private static Map<String, WeatherDataImportParser> defaultParsers() {
        final Map<String, WeatherDataImportParser> map = new LinkedHashMap<>();
        final CsvWeatherDataImportParser csv = new CsvWeatherDataImportParser();
        map.put(csv.supportedFormat().toUpperCase(), csv);
        return map;
    }

    public List<String> supportedFormats() {
        return List.copyOf(parsers.keySet());
    }

    public WeatherImportResult importFromFile(final String format, final Path filePath) {
        final WeatherDataImportParser parser = parsers.get(normalizeFormat(format));
        if (parser == null) {
            throw new UnsupportedImportFormatException(format);
        }
        if (!Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
            throw new ImportParseException("File not found or not readable: " + filePath);
        }

        final List<WeatherImportRow> rows;
        try (InputStream input = Files.newInputStream(filePath)) {
            rows = parser.parse(input);
        } catch (final IOException ex) {
            throw new ImportParseException("Failed to read file: " + filePath, ex);
        }

        if (rows.isEmpty()) {
            throw new ImportParseException("File contains no data rows");
        }

        final List<WeatherImportRowError> errors = new ArrayList<>();
        int imported = 0;

        for (final WeatherImportRow row : rows) {
            try {
                persistRow(row);
                imported++;
            } catch (final Exception ex) {
                errors.add(new WeatherImportRowError(row.lineNumber(), ex.getMessage()));
            }
        }

        return new WeatherImportResult(rows.size(), imported, errors.size(), List.copyOf(errors));
    }

    private void persistRow(final WeatherImportRow row) {
        final var code = AirControlAreaCode.valueOf(row.areaCode());
        if (areaRepo.findByCode(code).isEmpty()) {
            throw new IllegalArgumentException("Unknown air control area: " + row.areaCode());
        }
        if (observationExists(code, row.observedAt())) {
            throw new IllegalArgumentException(
                    "Weather observation already exists for area " + code + " at " + row.observedAt());
        }

        final WeatherSource source = row.source() == null || row.source().isBlank()
                ? WeatherSource.csvDefault()
                : new WeatherSource(row.source());

        final var weatherData = new WeatherData(
                code,
                row.observedAt(),
                row.windSpeedMs(),
                row.windDirectionDegrees(),
                row.temperatureC(),
                row.humidityPercent(),
                source);
        weatherRepo.save(weatherData);
    }

    private boolean observationExists(final AirControlAreaCode code, final Instant observedAt) {
        final Instant toExclusive = observedAt.plusNanos(1);
        for (final WeatherData existing : weatherRepo.findByAreaCodeBetween(code, observedAt, toExclusive)) {
            if (existing.observedAt().equals(observedAt)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeFormat(final String format) {
        if (format == null || format.isBlank()) {
            return "CSV";
        }
        return format.trim().toUpperCase();
    }
}
