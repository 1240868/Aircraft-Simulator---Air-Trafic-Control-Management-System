package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import java.lang.reflect.Constructor;
import java.time.Instant;
import org.junit.Test;

public class WeatherDataTest {

    private static final AirControlAreaCode AREA = AirControlAreaCode.valueOf("PT-LIS");
    private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");

    private WeatherData validData() {
        return new WeatherData(AREA, NOW, 10.5, 90, 22.3, 55);
    }

    @Test
    public void shouldCreateValidWeatherData() {
        final var wd = validData();
        assertEquals("PT-LIS", wd.areaCode().toString());
        assertEquals(NOW, wd.observedAt());
        assertEquals(10.5, wd.windSpeedMs(), 0.0001);
        assertEquals(90, wd.windDirectionDegrees(), 0.0001);
        assertEquals(22.3, wd.temperatureC(), 0.0001);
        assertEquals(55, wd.humidityPercent(), 0.0001);
        assertEquals("MANUAL", wd.source().value());
    }

    @Test
    public void shouldStoreExplicitSource() {
        final var wd = new WeatherData(AREA, NOW, 5, 90, 20, 50, WeatherSource.csvDefault());
        assertEquals("CSV-IMPORT", wd.source().value());
    }

    @Test
    public void shouldGenerateUniqueId() {
        assertNotNull(validData().identity());
    }

    @Test
    public void twoInstancesShouldHaveDifferentIds() {
        assertFalse(validData().identity().toString().equals(validData().identity().toString()));
    }

    @Test
    public void shouldAcceptZeroWindSpeed() {
        assertEquals(0, new WeatherData(AREA, NOW, 0, 0, 0, 0).windSpeedMs(), 0.0001);
    }

    @Test
    public void shouldAcceptZeroWindDirection() {
        assertEquals(0, new WeatherData(AREA, NOW, 5, 0, 20, 50).windDirectionDegrees(), 0.0001);
    }

    @Test
    public void shouldAcceptWindDirectionJustBelow360() {
        assertEquals(359.99, new WeatherData(AREA, NOW, 5, 359.99, 20, 50).windDirectionDegrees(), 0.0001);
    }

    @Test
    public void shouldAcceptNegativeTemperature() {
        assertEquals(-40, new WeatherData(AREA, NOW, 5, 180, -40, 50).temperatureC(), 0.0001);
    }

    @Test
    public void shouldAcceptZeroHumidity() {
        assertEquals(0, new WeatherData(AREA, NOW, 5, 180, 20, 0).humidityPercent(), 0.0001);
    }

    @Test
    public void shouldAcceptHundredHumidity() {
        assertEquals(100, new WeatherData(AREA, NOW, 5, 180, 20, 100).humidityPercent(), 0.0001);
    }

    @Test
    public void shouldRejectNullAreaCode() {
        assertThrows(NullPointerException.class, () -> new WeatherData(null, NOW, 5, 90, 20, 50));
    }

    @Test
    public void shouldRejectNullObservedAt() {
        assertThrows(NullPointerException.class, () -> new WeatherData(AREA, null, 5, 90, 20, 50));
    }

    @Test
    public void shouldRejectNegativeWindSpeed() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, -1, 90, 20, 50));
    }

    @Test
    public void shouldRejectNegativeWindDirection() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, 5, -1, 20, 50));
    }

    @Test
    public void shouldRejectWindDirectionAt360() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, 5, 360, 20, 50));
    }

    @Test
    public void shouldRejectWindDirectionAbove360() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, 5, 500, 20, 50));
    }

    @Test
    public void shouldRejectNegativeHumidity() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, 5, 90, 20, -1));
    }

    @Test
    public void shouldRejectHumidityAbove100() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData(AREA, NOW, 5, 90, 20, 101));
    }

    @Test
    public void sameAsSelfShouldReturnTrue() {
        final var wd = validData();
        assertTrue(wd.sameAs(wd));
    }

    @Test
    public void sameAsNullShouldReturnFalse() {
        assertFalse(validData().sameAs(null));
    }

    @Test
    public void sameAsDifferentTypeShouldReturnFalse() {
        assertFalse(validData().sameAs("string"));
    }

    @Test
    public void sameAsDifferentWeatherDataShouldReturnFalse() {
        final var a = validData();
        final var b = validData();
        assertFalse(a.sameAs(b));
    }

    @Test
    public void equalsSelfShouldReturnTrue() {
        final var wd = validData();
        assertTrue(wd.equals(wd));
    }

    @Test
    public void equalsNullShouldReturnFalse() {
        assertFalse(validData().equals(null));
    }

    @Test
    public void equalsDifferentTypeShouldReturnFalse() {
        assertFalse(validData().equals("string"));
    }

    @Test
    public void equalsDifferentWeatherDataShouldReturnFalse() {
        assertFalse(validData().equals(validData()));
    }

    @Test
    public void hashCodeShouldBeConsistent() {
        final var wd = validData();
        assertEquals(wd.hashCode(), wd.hashCode());
    }

    @Test
    public void protectedConstructorShouldExist() throws Exception {
        final Constructor<WeatherData> ctor = WeatherData.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final WeatherData wd = ctor.newInstance();
        assertNotNull(wd);
    }
}
