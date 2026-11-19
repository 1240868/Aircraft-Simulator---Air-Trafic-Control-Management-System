package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** US041/US042 — weather source value object. */
public class WeatherSourceTest {

    @Test
    public void shouldTrimValue() {
        assertEquals("METAR", new WeatherSource("  METAR  ").value());
    }

    @Test
    public void shouldRejectNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource(null));
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource(""));
    }

    @Test
    public void factoriesShouldProvideDefaults() {
        assertEquals("CSV-IMPORT", WeatherSource.csvDefault().value());
        assertEquals("MANUAL", WeatherSource.manualEntry().value());
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new WeatherSource());
    }
}
