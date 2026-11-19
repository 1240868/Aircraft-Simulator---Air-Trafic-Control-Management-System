package eapli.aisafe.airport.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eapli.aisafe.aircontrol.domain.AirControlArea;

public class AirportTest {

    private final AirControlArea airControlArea =
            new AirControlArea("AC01", "Area 1", -10.0, 10.0, -20.0, 20.0);

    @Test
    public void ensureAirportSameAsItsInstance() {
        final Airport airport = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        assertTrue(airport.sameAs(airport));
    }

    @Test
    public void ensureTwoAirportsWithSameIcaoAreTheSame() {
        final Airport a1 = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        final Airport a2 = new Airport("LPPT", "OPO", "Porto Airport", "Porto", "Portugal",
                41.237, -8.670, 69.0, airControlArea);

        assertTrue(a1.sameAs(a2));
    }

    @Test
    public void ensureTwoAirportsWithDifferentIcaoAreNotTheSame() {
        final Airport a1 = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        final Airport a2 = new Airport("LPPR", "OPO", "Porto Airport", "Porto", "Portugal",
                41.237, -8.670, 69.0, airControlArea);

        assertFalse(a1.sameAs(a2));
    }

    @Test
    public void ensureAirportNotSameAsDifferentObjectType() {
        final Airport airport = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        assertFalse(airport.sameAs(airControlArea));
    }

    @Test
    public void ensureGettersReturnExpectedValues() {
        final Airport airport = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        assertEquals("LPPT", airport.icaoCode());
        assertEquals("LIS", airport.iataCode());
        assertEquals("Lisboa Airport", airport.name());
        assertEquals("Lisboa", airport.town());
        assertEquals("Portugal", airport.country());
        assertEquals(38.774, airport.latitude(), 0.000001);
        assertEquals(-9.134, airport.longitude(), 0.000001);
        assertEquals(114.0, airport.elevationMeters(), 0.000001);
        assertEquals(airControlArea, airport.airControlArea());
    }

    @Test
    public void ensureToStringIsCorrect() {
        final Airport airport = new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);

        assertEquals("LPPT/LIS - Lisboa Airport", airport.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyIcaoCode() {
        new Airport("", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidIcaoLength() {
        new Airport("LPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyIataCode() {
        new Airport("LPPT", "", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidIataLength() {
        new Airport("LPPT", "LI", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyName() {
        new Airport("LPPT", "LIS", "", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyTown() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "", "Portugal",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForEmptyCountry() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "",
                38.774, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLatitudeTooLow() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                -91.0, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLatitudeTooHigh() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                91.0, -9.134, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLongitudeTooLow() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -181.0, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForInvalidLongitudeTooHigh() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, 181.0, 114.0, airControlArea);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureConstructorFailsForNullAirControlArea() {
        new Airport("LPPT", "LIS", "Lisboa Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, null);
    }
}