package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** ISO country code used when registering aircraft (US071 fleet flows). */
public class AircraftRegistrationCountryTest {

    @Test
    public void valueOfShouldNormalizeToUpperCase() {
        assertEquals("PT", AircraftRegistrationCountry.valueOf("  pt  ").toString());
    }

    @Test
    public void valueOfShouldRejectNonAlpha2() {
        assertThrows(IllegalArgumentException.class, () -> AircraftRegistrationCountry.valueOf("PRT"));
        assertThrows(IllegalArgumentException.class, () -> AircraftRegistrationCountry.valueOf("P1"));
    }

    @Test
    public void equalsAndHashCode() {
        final var a = AircraftRegistrationCountry.valueOf("PT");
        final var b = AircraftRegistrationCountry.valueOf("PT");
        final var c = AircraftRegistrationCountry.valueOf("ES");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsShouldBeFalseForNullAndWrongType() {
        final var a = AircraftRegistrationCountry.valueOf("PT");
        assertFalse(a.equals(null));
        assertFalse(a.equals("PT"));
    }

    @Test
    public void equalsShouldBeReflexive() {
        final var a = AircraftRegistrationCountry.valueOf("DE");
        assertTrue(a.equals(a));
    }

    @Test
    public void valueOfShouldRejectEmptyOrInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AircraftRegistrationCountry.valueOf(""));
        assertThrows(IllegalArgumentException.class, () -> AircraftRegistrationCountry.valueOf("   "));
        assertThrows(IllegalArgumentException.class, () -> AircraftRegistrationCountry.valueOf("p"));
    }

    @Test
    public void protectedConstructorForOrm() throws Exception {
        final var c = AircraftRegistrationCountry.class.getDeclaredConstructor();
        c.setAccessible(true);
        assertNotNull(c.newInstance());
    }
}
