package eapli.aisafe.weather.application.importing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

/** US042 — CSV bulk weather parser. */
public class CsvWeatherDataImportParserTest {

    private final CsvWeatherDataImportParser parser = new CsvWeatherDataImportParser();

    @Test
    public void shouldParseDataRowWithoutHeader() {
        final String csv = "PT-LIS;2026-05-29T14:00;18.5;62;5.2;270;METAR-Provider\n";
        final List<WeatherImportRow> rows = parser.parse(stream(csv));
        assertEquals(1, rows.size());
        final WeatherImportRow row = rows.get(0);
        assertEquals("PT-LIS", row.areaCode());
        assertEquals(Instant.parse("2026-05-29T14:00:00Z"), row.observedAt());
        assertEquals(18.5, row.temperatureC(), 0.001);
        assertEquals("METAR-Provider", row.source());
    }

    @Test
    public void shouldSkipHeaderAndUseDefaultSourceWhenMissing() {
        final String csv = """
            areaCode;observedAt;temperatureC;humidityPercent;windSpeedMs;windDirectionDegrees;source
            PT-OPO;2026-05-29T10:00;10;50;3;90
            """;
        final List<WeatherImportRow> rows = parser.parse(stream(csv));
        assertEquals(1, rows.size());
        assertEquals("PT-OPO", rows.get(0).areaCode());
        assertEquals(null, rows.get(0).source());
    }

    @Test
    public void shouldRejectTooFewColumns() {
        assertThrows(ImportParseException.class,
                () -> parser.parse(stream("PT-LIS;2026-05-29T14:00;18.5\n")));
    }

    @Test
    public void supportedFormatShouldBeCsv() {
        assertEquals("CSV", parser.supportedFormat());
    }

    @Test
    public void emptyFileShouldReturnNoRows() {
        assertTrue(parser.parse(stream("")).isEmpty());
    }

    @Test
    public void shouldRejectInvalidObservedAt() {
        assertThrows(ImportParseException.class,
                () -> parser.parse(stream("PT-LIS;not-a-date;18;60;5;180\n")));
    }

    @Test
    public void shouldSkipBlankLines() {
        final String csv = """
            
            PT-LIS;2026-05-29T14:00;18;60;5;180
            
            """;
        assertEquals(1, parser.parse(stream(csv)).size());
    }

    private static ByteArrayInputStream stream(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
