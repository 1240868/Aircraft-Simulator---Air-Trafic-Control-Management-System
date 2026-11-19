package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AircraftTypeTest {

    @Test
    public void shouldHaveThreeValues() {
        assertEquals(3, AircraftType.values().length);
    }

    @Test
    public void valueOfShouldResolveEachConstant() {
        assertEquals(AircraftType.PASSENGER, AircraftType.valueOf("PASSENGER"));
        assertEquals(AircraftType.CARGO, AircraftType.valueOf("CARGO"));
        assertEquals(AircraftType.MIXED, AircraftType.valueOf("MIXED"));
    }

    @Test
    public void nameShouldMatchConstant() {
        for (final AircraftType t : AircraftType.values()) {
            assertEquals(t.name(), t.toString());
        }
    }

    @Test
    public void valueOfShouldRejectUnknown() {
        assertThrows(IllegalArgumentException.class, () -> AircraftType.valueOf("JET"));
    }

    @Test
    public void ordinalShouldBeStable() {
        assertTrue(AircraftType.PASSENGER.ordinal() >= 0);
        assertTrue(AircraftType.CARGO.ordinal() >= 0);
        assertTrue(AircraftType.MIXED.ordinal() >= 0);
    }
}
