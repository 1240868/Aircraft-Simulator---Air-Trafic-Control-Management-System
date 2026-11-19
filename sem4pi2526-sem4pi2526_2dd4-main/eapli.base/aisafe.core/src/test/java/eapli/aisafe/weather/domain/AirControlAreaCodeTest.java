package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import org.junit.Test;

public class AirControlAreaCodeTest {

    @Test
    public void ensureAirControlAreaCodeEqualsPassesForSameValue() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");
        final AirControlAreaCode code2 = new AirControlAreaCode("AC01");

        assertTrue(code1.equals(code2));
    }

    @Test
    public void ensureAirControlAreaCodeEqualsFailsForDifferentValue() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");
        final AirControlAreaCode code2 = new AirControlAreaCode("AC02");

        assertFalse(code1.equals(code2));
    }

    @Test
    public void ensureAirControlAreaCodeEqualsIsTrueForSameInstance() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");

        assertTrue(code1.equals(code1));
    }

    @Test
    public void ensureAirControlAreaCodeEqualsFailsForDifferentObjectTypes() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");

        assertFalse(code1.equals("AC01"));
    }

    @Test
    public void ensureCompareToReturnsZeroForSameValue() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");
        final AirControlAreaCode code2 = new AirControlAreaCode("AC01");

        assertEquals(0, code1.compareTo(code2));
    }

    @Test
    public void ensureCompareToReturnsNegativeWhenSmaller() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");
        final AirControlAreaCode code2 = new AirControlAreaCode("AC02");

        assertTrue(code1.compareTo(code2) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveWhenGreater() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC02");
        final AirControlAreaCode code2 = new AirControlAreaCode("AC01");

        assertTrue(code1.compareTo(code2) > 0);
    }

    @Test
    public void ensureValueIsTrimmedAndUppercased() {
        final AirControlAreaCode code = new AirControlAreaCode("  ac01  ");

        assertEquals("AC01", code.toString());
    }

    @Test
    public void ensureValueOfCreatesCorrectCode() {
        final AirControlAreaCode code = AirControlAreaCode.valueOf("ac01");

        assertEquals("AC01", code.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForNullValue() {
        new AirControlAreaCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyValue() {
        new AirControlAreaCode("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForBlankValue() {
        new AirControlAreaCode("   ");
    }

    @Test
    public void ensureHashCodeIsEqualForEqualObjects() {
        final AirControlAreaCode code1 = new AirControlAreaCode("AC01");
        final AirControlAreaCode code2 = new AirControlAreaCode("ac01");

        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    public void ensureToStringReturnsNormalizedValue() {
        final AirControlAreaCode code = new AirControlAreaCode("  ac01  ");

        assertEquals("AC01", code.toString());
    }
}