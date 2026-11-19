package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.UUID;
import org.junit.Test;

public class EngineModelIdTest {

    @Test
    public void newIdShouldBeNonNull() {
        assertNotNull(EngineModelId.newId());
    }

    @Test
    public void shouldRejectNullUuid() {
        assertThrows(NullPointerException.class, () -> new EngineModelId(null));
    }

    @Test
    public void asUuidShouldRoundTrip() {
        final UUID u = UUID.randomUUID();
        assertEquals(u, new EngineModelId(u).asUuid());
    }

    @Test
    public void toStringShouldMatchUuidString() {
        final UUID u = UUID.randomUUID();
        assertEquals(u.toString(), new EngineModelId(u).toString());
    }

    @Test
    public void compareToLess() {
        final var low = new EngineModelId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        final var high = new EngineModelId(UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff"));
        assertTrue(low.compareTo(high) < 0);
    }

    @Test
    public void compareToGreater() {
        final var low = new EngineModelId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        final var high = new EngineModelId(UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff"));
        assertTrue(high.compareTo(low) > 0);
    }

    @Test
    public void compareToEqual() {
        final UUID u = UUID.randomUUID();
        assertEquals(0, new EngineModelId(u).compareTo(new EngineModelId(u)));
    }

    @Test
    public void equalsSameInstance() {
        final var id = EngineModelId.newId();
        assertEquals(id, id);
    }

    @Test
    public void equalsNull() {
        assertFalse(EngineModelId.newId().equals(null));
    }

    @Test
    public void equalsDifferentType() {
        assertFalse(EngineModelId.newId().equals("uuid"));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<EngineModelId> ctor = EngineModelId.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
