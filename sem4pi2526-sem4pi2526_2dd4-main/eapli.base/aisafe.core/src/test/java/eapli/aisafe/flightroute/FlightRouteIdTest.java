package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

/** US073 — flight route identifier. */
public class FlightRouteIdTest {

    @Test
    public void newIdShouldGenerateUniqueValues() {
        final FlightRouteId a = FlightRouteId.newId();
        final FlightRouteId b = FlightRouteId.newId();
        assertNotEquals(a, b);
        assertTrue(a.toString().length() > 0);
    }

    @Test
    public void shouldCompareAndEqualByUuid() {
        final UUID uuid = UUID.randomUUID();
        final FlightRouteId left = new FlightRouteId(uuid);
        final FlightRouteId right = new FlightRouteId(uuid);
        assertEquals(left, right);
        assertEquals(0, left.compareTo(right));
        assertEquals(uuid, left.asUuid());
    }

    @Test
    public void shouldRejectNullUuid() {
        assertThrows(NullPointerException.class, () -> new FlightRouteId(null));
    }

    @Test
    public void equalsShouldHandleSelfNullAndOtherType() {
        final FlightRouteId id = FlightRouteId.newId();
        assertEquals(id, id);
        assertNotEquals(id, null);
        assertNotEquals(id, "not an id");
    }

    @Test
    public void equalsShouldBeFalseForDifferentValues() {
        assertNotEquals(FlightRouteId.newId(), FlightRouteId.newId());
    }

    @Test
    public void hashCodeShouldBeConsistent() {
        final FlightRouteId id = FlightRouteId.newId();
        assertEquals(id.hashCode(), id.hashCode());
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new FlightRouteId());
    }
}
