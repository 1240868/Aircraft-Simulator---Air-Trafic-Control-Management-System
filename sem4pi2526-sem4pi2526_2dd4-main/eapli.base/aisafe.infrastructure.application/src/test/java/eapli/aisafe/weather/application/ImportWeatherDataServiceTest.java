package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.weather.application.importing.CsvWeatherDataImportParser;
import eapli.aisafe.weather.application.importing.ImportParseException;
import eapli.aisafe.weather.application.importing.UnsupportedImportFormatException;
import eapli.aisafe.weather.application.importing.WeatherDataImportParser;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** US042 — bulk import service. */
public class ImportWeatherDataServiceTest {

    @Mock
    private WeatherDataRepository weatherRepo;
    @Mock
    private AirControlAreaRepository areaRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void supportedFormatsShouldExposeRegisteredParsers() {
        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        assertEquals(List.of("CSV"), service.supportedFormats());
    }

    @Test
    public void blankFormatShouldDefaultToCsv() throws Exception {
        when(areaRepo.findByCode(any())).thenReturn(Optional.of(
                new AirControlArea("PT-LIS", "Lisbon", 40, 42, -9, -7)));

        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        final Path file = Files.createTempFile("weather-default-format", ".csv");
        Files.writeString(file,
                "PT-LIS;2026-05-29T14:00;18;60;5;180\n",
                StandardCharsets.UTF_8);

        final var result = service.importFromFile("  ", file);
        Files.deleteIfExists(file);

        assertEquals(1, result.importedCount());
        verify(weatherRepo).save(any());
    }

    @Test
    public void unsupportedFormatShouldFail() {
        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        assertThrows(UnsupportedImportFormatException.class,
                () -> service.importFromFile("XML", Path.of("any.csv")));
    }

    @Test
    public void missingFileShouldFail() {
        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        assertThrows(ImportParseException.class,
                () -> service.importFromFile("CSV", Path.of("no-such-weather-file.csv")));
    }

    @Test
    public void customParserMapShouldBeHonoured() throws Exception {
        final WeatherDataImportParser parser = mock(WeatherDataImportParser.class);
        when(parser.supportedFormat()).thenReturn("CUSTOM");
        when(parser.parse(any())).thenReturn(List.of());

        final Map<String, WeatherDataImportParser> parsers = new LinkedHashMap<>();
        parsers.put("CUSTOM", parser);

        final var service = new ImportWeatherDataService(weatherRepo, areaRepo, parsers);
        assertThrows(ImportParseException.class,
                () -> service.importFromFile("CUSTOM", Path.of("missing-custom.csv")));
        verify(parser, never()).parse(any());
    }

    @Test
    public void emptyDataRowsShouldFail() throws Exception {
        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        final Path file = Files.createTempFile("weather-empty-rows", ".csv");
        Files.writeString(file, "\n\n", StandardCharsets.UTF_8);

        assertThrows(ImportParseException.class, () -> service.importFromFile("CSV", file));
        Files.deleteIfExists(file);
        verify(weatherRepo, never()).save(any());
    }

    @Test
    public void unknownAreaShouldBeRejectedPerRow() throws Exception {
        when(areaRepo.findByCode(AirControlAreaCode.valueOf("PT-LIS"))).thenReturn(Optional.empty());

        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        final Path file = Files.createTempFile("weather-unknown-area", ".csv");
        Files.writeString(file,
                "PT-LIS;2026-05-29T14:00;18;60;5;180\n",
                StandardCharsets.UTF_8);

        final var result = service.importFromFile("CSV", file);
        Files.deleteIfExists(file);

        assertEquals(0, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertTrue(result.errors().get(0).message().contains("Unknown air control area"));
    }

    @Test
    public void constructorWithDefaultParsersShouldIncludeCsv() {
        final var service = new ImportWeatherDataService(weatherRepo, areaRepo);
        assertTrue(service.supportedFormats().contains(new CsvWeatherDataImportParser().supportedFormat()));
    }
}
