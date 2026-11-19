package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class FlightPlanIdTest {

    @Test
    public void newIdShouldReturnNonNull() {
        assertNotNull(FlightPlanId.newId());
    }

    @Test
    public void constructorShouldAcceptUUID() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new FlightPlanId(uuid).asUuid());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUUID() {
        new FlightPlanId(null);
    }

    @Test
    public void equalsSameValue() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(new FlightPlanId(uuid), new FlightPlanId(uuid));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(FlightPlanId.newId(), FlightPlanId.newId());
    }

    @Test
    public void compareToEqual() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(0, new FlightPlanId(uuid).compareTo(new FlightPlanId(uuid)));
    }

    @Test
    public void compareToLess() {
        final FlightPlanId a = new FlightPlanId(new UUID(0, 1));
        final FlightPlanId b = new FlightPlanId(new UUID(0, 2));
        assertTrue(a.compareTo(b) < 0);
    }
}
