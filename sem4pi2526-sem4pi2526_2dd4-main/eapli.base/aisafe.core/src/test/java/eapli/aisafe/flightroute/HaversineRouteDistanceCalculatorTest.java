package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.airport.domain.Airport;
import org.junit.Test;

/** US073 — distance and duration estimation. */
public class HaversineRouteDistanceCalculatorTest {

    @Test
    public void shouldComputePositiveDistanceAndDuration() {
        final var area = new AirControlArea("PT-TEST", "Test", 40, 42, -9, -7);
        final var lis = new Airport("LPPT", "LIS", "Lisbon", "Lisbon", "PT",
                38.7742, -9.1342, 100, area);
        final var opo = new Airport("LPPR", "OPO", "Porto", "Porto", "PT",
                41.2481, -8.6814, 100, area);

        final RouteDistance distance = HaversineRouteDistanceCalculator.calculateDistance(lis, opo);
        final RouteDuration duration = HaversineRouteDistanceCalculator.estimateDuration(distance);

        assertTrue(distance.value() > 200_000);
        assertTrue(duration.minutes() > 0);
        // LIS->OPO is ~277 km; at 850 km/h that is ~20 min. Guards against the
        // meters/km unit bug that previously inflated duration ~1000x.
        assertTrue("duration should be realistic (got " + duration.minutes() + " min)",
                duration.minutes() < 60);
    }

    @Test
    public void estimateDurationShouldBeAtLeastOneMinute() {
        final var shortHop = new RouteDistance(1000, eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit.METER);
        assertTrue(HaversineRouteDistanceCalculator.estimateDuration(shortHop).minutes() >= 1L);
    }
}
