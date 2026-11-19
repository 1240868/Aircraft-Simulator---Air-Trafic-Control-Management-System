package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherSource;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** US043 — weather consultation service. */
public class ConsultWeatherDataServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 29);

    @Mock
    private WeatherDataRepository weatherRepo;
    @Mock
    private AirControlAreaRepository areaRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void consultShouldMapNullSourceToNa() {
        when(areaRepo.findByCode(any())).thenReturn(Optional.of(
                new AirControlArea("PT-LIS", "Lisbon", 40, 42, -9, -7)));

        final WeatherData legacy = mock(WeatherData.class);
        when(legacy.identity()).thenReturn(WeatherDataId.newId());
        when(legacy.areaCode()).thenReturn(AirControlAreaCode.valueOf("PT-LIS"));
        when(legacy.observedAt()).thenReturn(Instant.parse("2026-05-29T10:00:00Z"));
        when(legacy.temperatureC()).thenReturn(15.0);
        when(legacy.humidityPercent()).thenReturn(55.0);
        when(legacy.windSpeedMs()).thenReturn(3.0);
        when(legacy.windDirectionDegrees()).thenReturn(90.0);
        when(legacy.source()).thenReturn(null);

        when(weatherRepo.findByAreaCodeBetween(
                eq(AirControlAreaCode.valueOf("PT-LIS")), any(), any()))
                .thenReturn(List.of(legacy));

        final var service = new ConsultWeatherDataService(weatherRepo, areaRepo);
        final var result = service.consult("PT-LIS", DAY);

        assertEquals(1, result.recordCount());
        assertEquals("N/A", result.records().get(0).source());
    }

    @Test
    public void consultShouldRejectUnknownArea() {
        when(areaRepo.findByCode(any())).thenReturn(Optional.empty());
        final var service = new ConsultWeatherDataService(weatherRepo, areaRepo);

        assertThrows(IllegalArgumentException.class, () -> service.consult("XX-UNK", DAY));
    }

    @Test
    public void consultShouldExposeSourceValueWhenPresent() {
        when(areaRepo.findByCode(any())).thenReturn(Optional.of(
                new AirControlArea("PT-LIS", "Lisbon", 40, 42, -9, -7)));

        final var record = new WeatherData(
                AirControlAreaCode.valueOf("PT-LIS"),
                Instant.parse("2026-05-29T12:00:00Z"),
                4, 180, 18, 60,
                WeatherSource.manualEntry());

        when(weatherRepo.findByAreaCodeBetween(any(), any(), any())).thenReturn(List.of(record));

        final var service = new ConsultWeatherDataService(weatherRepo, areaRepo);
        final var result = service.consult("pt-lis", DAY);

        assertEquals("MANUAL", result.records().get(0).source());
        assertEquals("PT-LIS", result.areaCode());
    }
}
