package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** US073 — route duration value object. */
public class RouteDurationTest {

    @Test
    public void shouldStorePositiveMinutes() {
        assertEquals(90L, new RouteDuration(90).minutes());
    }

    @Test
    public void shouldRejectZeroOrNegativeMinutes() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDuration(0));
        assertThrows(IllegalArgumentException.class, () -> new RouteDuration(-5));
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new RouteDuration());
    }
}
