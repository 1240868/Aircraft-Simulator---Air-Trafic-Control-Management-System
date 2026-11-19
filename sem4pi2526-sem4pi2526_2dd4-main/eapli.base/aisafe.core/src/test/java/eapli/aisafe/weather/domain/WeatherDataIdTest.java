package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.UUID;
import org.junit.Test;

public class WeatherDataIdTest {

    @Test
    public void newIdShouldCreateNonNull() {
        assertNotNull(WeatherDataId.newId());
    }

    @Test
    public void newIdShouldProduceUniqueValues() {
        assertNotEquals(WeatherDataId.newId().toString(), WeatherDataId.newId().toString());
    }

    @Test
    public void shouldCreateFromUuid() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), new WeatherDataId(uuid).toString());
    }

    @Test
    public void shouldRejectNullUuid() {
        assertThrows(NullPointerException.class, () -> new WeatherDataId(null));
    }

    @Test
    public void asUuidShouldRoundTrip() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid, new WeatherDataId(uuid).asUuid());
    }

    @Test
    public void toStringShouldReturnUuidString() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), new WeatherDataId(uuid).toString());
    }

    @Test
    public void compareToShouldOrder() {
        final var id1 = new WeatherDataId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final var id2 = new WeatherDataId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        assertTrue(id1.compareTo(id2) < 0);
    }

    @Test
    public void compareToReverse() {
        final var id1 = new WeatherDataId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final var id2 = new WeatherDataId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        assertTrue(id2.compareTo(id1) > 0);
    }

    @Test
    public void compareToSameValue() {
        final var uuid = UUID.randomUUID();
        assertEquals(0, new WeatherDataId(uuid).compareTo(new WeatherDataId(uuid)));
    }

    @Test
    public void protectedConstructorShouldExist() throws Exception {
        final Constructor<WeatherDataId> ctor = WeatherDataId.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
