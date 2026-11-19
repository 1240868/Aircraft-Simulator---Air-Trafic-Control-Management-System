package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import org.junit.Test;

/** US073 — route distance value object. */
public class RouteDistanceTest {

    @Test
    public void shouldAcceptMetersAndMiles() {
        final var meters = new RouteDistance(1000, MeasurementUnit.METER);
        assertEquals(1000, meters.value(), 0.001);
        assertEquals(MeasurementUnit.METER, meters.unit());

        final var miles = new RouteDistance(10, MeasurementUnit.MILE);
        assertEquals(MeasurementUnit.MILE, miles.unit());
    }

    @Test
    public void shouldRejectNegativeDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDistance(-1, MeasurementUnit.METER));
    }

    @Test
    public void shouldRejectInvalidUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDistance(100, MeasurementUnit.KILOGRAM));
    }

    @Test
    public void shouldAcceptZeroDistance() {
        assertEquals(0, new RouteDistance(0, MeasurementUnit.METER).value(), 0.001);
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new RouteDistance());
    }
}
