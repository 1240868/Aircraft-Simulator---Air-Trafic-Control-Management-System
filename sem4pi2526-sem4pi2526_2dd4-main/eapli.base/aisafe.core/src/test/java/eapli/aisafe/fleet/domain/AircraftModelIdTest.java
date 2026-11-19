package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.UUID;
import org.junit.Test;

public class AircraftModelIdTest {

    @Test
    public void newIdShouldBeNonNull() {
        assertNotNull(AircraftModelId.newId());
    }

    @Test
    public void shouldRejectNullUuid() {
        assertThrows(NullPointerException.class, () -> new AircraftModelId(null));
    }

    @Test
    public void shouldRoundTripThroughString() {
        final UUID u = UUID.randomUUID();
        assertEquals(u, UUID.fromString(new AircraftModelId(u).toString()));
    }

    @Test
    public void toStringShouldBeUuidString() {
        final UUID u = UUID.randomUUID();
        assertEquals(u.toString(), new AircraftModelId(u).toString());
    }

    @Test
    public void equalsSameValue() {
        final UUID u = UUID.randomUUID();
        assertEquals(new AircraftModelId(u), new AircraftModelId(u));
    }

    @Test
    public void equalsSameInstance() {
        final var id = AircraftModelId.newId();
        assertEquals(id, id);
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(AircraftModelId.newId(), AircraftModelId.newId());
    }

    @Test
    public void equalsNull() {
        assertFalse(AircraftModelId.newId().equals(null));
    }

    @Test
    public void equalsDifferentType() {
        assertFalse(AircraftModelId.newId().equals("not-an-id"));
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        final UUID u = UUID.randomUUID();
        assertEquals(new AircraftModelId(u).hashCode(), new AircraftModelId(u).hashCode());
    }

    @Test
    public void compareToLess() {
        final var low = new AircraftModelId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        final var high = new AircraftModelId(UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff"));
        assertTrue(low.compareTo(high) < 0);
    }

    @Test
    public void compareToGreater() {
        final var low = new AircraftModelId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        final var high = new AircraftModelId(UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff"));
        assertTrue(high.compareTo(low) > 0);
    }

    @Test
    public void compareToEqual() {
        final UUID u = UUID.randomUUID();
        assertEquals(0, new AircraftModelId(u).compareTo(new AircraftModelId(u)));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<AircraftModelId> ctor = AircraftModelId.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
