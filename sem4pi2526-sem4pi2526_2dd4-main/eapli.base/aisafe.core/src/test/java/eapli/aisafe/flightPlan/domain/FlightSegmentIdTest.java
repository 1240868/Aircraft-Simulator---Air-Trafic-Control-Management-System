package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class FlightSegmentIdTest {

    @Test
    public void newIdShouldReturnNonNull() {
        assertNotNull(FlightSegmentId.newId());
    }

    @Test
    public void constructorShouldAcceptUUID() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new FlightSegmentId(uuid).asUuid());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUUID() {
        new FlightSegmentId(null);
    }

    @Test
    public void equalsSameValue() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(new FlightSegmentId(uuid), new FlightSegmentId(uuid));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(FlightSegmentId.newId(), FlightSegmentId.newId());
    }

    @Test
    public void compareToEqual() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(0, new FlightSegmentId(uuid).compareTo(new FlightSegmentId(uuid)));
    }

    @Test
    public void compareToLess() {
        final FlightSegmentId a = new FlightSegmentId(new UUID(0, 1));
        final FlightSegmentId b = new FlightSegmentId(new UUID(0, 2));
        assertTrue(a.compareTo(b) < 0);
    }
}
