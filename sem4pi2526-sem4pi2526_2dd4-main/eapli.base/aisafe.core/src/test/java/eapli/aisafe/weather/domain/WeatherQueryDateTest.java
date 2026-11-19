package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.Test;

/** US043 — UTC day window for weather consultation. */
public class WeatherQueryDateTest {

    @Test
    public void shouldDefineUtcDayBoundaries() {
        final var qd = new WeatherQueryDate(LocalDate.of(2026, 5, 29));
        assertEquals(Instant.parse("2026-05-29T00:00:00Z"), qd.startOfDayUtc());
        assertEquals(Instant.parse("2026-05-30T00:00:00Z"), qd.endOfDayExclusiveUtc());
        assertEquals(LocalDate.of(2026, 5, 29), qd.value());
    }

    @Test
    public void shouldRejectNullDate() {
        assertThrows(NullPointerException.class, () -> new WeatherQueryDate(null));
    }
}
