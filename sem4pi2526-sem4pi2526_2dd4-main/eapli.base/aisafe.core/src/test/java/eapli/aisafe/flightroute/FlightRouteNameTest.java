package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** US073 — flight route name value object. */
public class FlightRouteNameTest {

    @Test
    public void composeShouldBuildValidName() {
        final var name = FlightRouteName.compose("tp", "123");
        assertEquals("TP123", name.value());
    }

    @Test
    public void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new FlightRouteName("TP12A"));
        assertThrows(IllegalArgumentException.class, () -> FlightRouteName.compose("TP", "12345"));
        assertThrows(IllegalArgumentException.class, () -> FlightRouteName.compose("T", "1"));
    }

    @Test
    public void constructorShouldTrimAndUpperCase() {
        assertEquals("TP123", new FlightRouteName(" tp123 ").value());
    }

    @Test
    public void shouldRejectNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new FlightRouteName(null));
        assertThrows(IllegalArgumentException.class, () -> new FlightRouteName("   "));
    }

    @Test
    public void composeShouldRejectEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> FlightRouteName.compose("", "1"));
        assertThrows(IllegalArgumentException.class, () -> FlightRouteName.compose("TP", ""));
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new FlightRouteName());
    }
}
