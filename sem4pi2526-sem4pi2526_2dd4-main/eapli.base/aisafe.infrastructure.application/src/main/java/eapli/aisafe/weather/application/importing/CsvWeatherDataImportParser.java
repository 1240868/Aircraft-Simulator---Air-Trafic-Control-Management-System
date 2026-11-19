package eapli.aisafe.weather.application.importing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Semicolon-separated CSV bulk weather import (US042 initial provider). */
public class CsvWeatherDataImportParser implements WeatherDataImportParser {

    private static final String FORMAT = "CSV";
    private static final char SEPARATOR = ';';
    private static final int REQUIRED_COLUMNS = 6;

    @Override
    public String supportedFormat() {
        return FORMAT;
    }

    @Override
    public List<WeatherImportRow> parse(final InputStream input) {
        final List<WeatherImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!headerSkipped && isHeaderLine(trimmed)) {
                    headerSkipped = true;
                    continue;
                }
                rows.add(parseDataLine(trimmed, lineNumber));
            }
        } catch (final IOException ex) {
            throw new ImportParseException("Failed to read import file", ex);
        }
        return rows;
    }

    private static boolean isHeaderLine(final String line) {
        final String first = line.split(String.valueOf(SEPARATOR), -1)[0].trim();
        return first.equalsIgnoreCase("areaCode");
    }

    private static WeatherImportRow parseDataLine(final String line, final int lineNumber) {
        final String[] parts = line.split(String.valueOf(SEPARATOR), -1);
        if (parts.length < REQUIRED_COLUMNS) {
            throw new ImportParseException(
                    "Line " + lineNumber + ": expected at least " + REQUIRED_COLUMNS + " columns");
        }
        try {
            final String areaCode = parts[0].trim();
            final Instant observedAt = LocalDateTime.parse(parts[1].trim())
                    .toInstant(ZoneOffset.UTC);
            final double temperatureC = Double.parseDouble(parts[2].trim());
            final double humidityPercent = Double.parseDouble(parts[3].trim());
            final double windSpeedMs = Double.parseDouble(parts[4].trim());
            final double windDirectionDegrees = Double.parseDouble(parts[5].trim());
            final String source = parts.length > 6 && !parts[6].trim().isEmpty()
                    ? parts[6].trim()
                    : null;
            return new WeatherImportRow(
                    lineNumber,
                    areaCode,
                    observedAt,
                    temperatureC,
                    humidityPercent,
                    windSpeedMs,
                    windDirectionDegrees,
                    source);
        } catch (final DateTimeParseException | NumberFormatException ex) {
            throw new ImportParseException("Line " + lineNumber + ": " + ex.getMessage(), ex);
        }
    }
}
