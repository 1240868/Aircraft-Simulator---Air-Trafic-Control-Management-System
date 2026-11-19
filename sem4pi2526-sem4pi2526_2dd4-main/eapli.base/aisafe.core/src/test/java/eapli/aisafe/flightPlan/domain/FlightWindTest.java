package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;

import org.junit.Test;

public class FlightWindTest {

    @Test
    public void shouldCreateValidFlightWind() {
        final WindSpeed speed = new WindSpeed(25.0, "KNOT");
        final WindDirection dir = new WindDirection(180.0);
        final FlightWind wind = new FlightWind(speed, dir, MeasurementUnit.KILOGRAM);
        assertNotNull(wind);
        assertEquals(MeasurementUnit.KILOGRAM, wind.unit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeWindSpeed() {
        new WindSpeed(-1.0, "KNOT");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyWindSpeedUnit() {
        new WindSpeed(25.0, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidWindDirection() {
        new WindDirection(400.0);
    }
}
