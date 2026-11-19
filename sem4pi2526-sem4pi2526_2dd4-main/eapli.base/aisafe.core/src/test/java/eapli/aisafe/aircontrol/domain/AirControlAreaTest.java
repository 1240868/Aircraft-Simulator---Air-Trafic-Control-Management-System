package eapli.aisafe.aircontrol.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirControlAreaTest {

    @Test
    public void ensureAirControlAreaSameAsItsInstance() {
        final AirControlArea aca = new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);

        assertTrue(aca.sameAs(aca));
    }

    @Test
    public void ensureTwoAirControlAreasWithSameCodeAreTheSame() {
        final AirControlArea aca1 = new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);
        final AirControlArea aca2 = new AirControlArea("AC01", "Area 2", -5.0, 5.0, -10.0, 10.0);

        assertTrue(aca1.sameAs(aca2));
    }

    @Test
    public void ensureTwoAirControlAreasWithDifferentCodesAreNotTheSame() {
        final AirControlArea aca1 = new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);
        final AirControlArea aca2 = new AirControlArea("AC02", "Area 2", -5.0, 5.0, -10.0, 10.0);

        assertFalse(aca1.sameAs(aca2));
    }

    @Test
    public void ensureGettersReturnValues() {
        final AirControlArea aca = new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);

        assertEquals(AirControlAreaCode.valueOf("AC01"), aca.identity());
        assertEquals(AirControlAreaCode.valueOf("AC01"), aca.areaCode());
        assertEquals("Area 1", aca.name());
        assertEquals(-10.0, aca.latMin(), 0.0);
        assertEquals(10.0, aca.latMax(), 0.0);
        assertEquals(-20.0, aca.lonMin(), 0.0);
        assertEquals(20.0, aca.lonMax(), 0.0);
    }

    @Test
    public void ensureToStringIsCorrect() {
        final AirControlArea aca = new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);

        assertEquals("AC01 - Area 1", aca.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyName() {
        new AirControlArea("AC01", "", -10.0, 10.0, -20.0, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLatMin() {
        new AirControlArea("AC01", "Area 1", -91.0, 10.0, -20.0, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLatMax() {
        new AirControlArea("AC01", "Area 1", -10.0, 91.0, -20.0, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsWhenLatMinIsNotLessThanLatMax() {
        new AirControlArea("AC01", "Area 1", 10.0, 10.0, -20.0, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLonMin() {
        new AirControlArea("AC01", "Area 1", -10.0, 10.0, -181.0, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLonMax() {
        new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 181.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsWhenLonMinIsNotLessThanLonMax() {
        new AirControlArea("AC01", "Area 1", -10.0, 10.0, 20.0, 20.0);
    }
}