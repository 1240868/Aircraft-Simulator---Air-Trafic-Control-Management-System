package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Used by {@link Aircraft} lifecycle (US071). */
public class AircraftOperationalStatusTest {

    @Test
    public void valueOfShouldResolveAllConstants() {
        for (final AircraftOperationalStatus s : AircraftOperationalStatus.values()) {
            assertEquals(s, AircraftOperationalStatus.valueOf(s.name()));
        }
    }

    @Test
    public void valuesShouldExposeThreeStates() {
        assertEquals(3, AircraftOperationalStatus.values().length);
    }

    @Test
    public void valueOfShouldRejectUnknown() {
        assertThrows(IllegalArgumentException.class, () -> AircraftOperationalStatus.valueOf("UNKNOWN"));
    }

    @Test
    public void compareToShouldOrderByDeclaration() {
        assertTrue(AircraftOperationalStatus.ACTIVE.compareTo(AircraftOperationalStatus.MAINTENANCE) < 0);
        assertTrue(AircraftOperationalStatus.MAINTENANCE.compareTo(AircraftOperationalStatus.RETIRED) < 0);
        assertEquals(0, AircraftOperationalStatus.ACTIVE.compareTo(AircraftOperationalStatus.ACTIVE));
    }
}
