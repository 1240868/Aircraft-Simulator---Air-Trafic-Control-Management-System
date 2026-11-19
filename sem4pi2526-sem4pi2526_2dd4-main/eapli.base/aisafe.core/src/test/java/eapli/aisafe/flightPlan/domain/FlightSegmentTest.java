package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.sharedkernel.valueobjects.Altitude;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;

import org.junit.Test;
import org.springframework.data.geo.Distance;

import java.time.Duration;

public class FlightSegmentTest {

    @Test
    public void shouldCreateValidSegment() {
        final FlightSegmentId id = FlightSegmentId.newId();
        final Distance dist = new Distance(100.0);
        final Duration dur = Duration.ofMinutes(30);
        final Altitude alt = new Altitude(10000.0, MeasurementUnit.METER);
        final FlightSegment seg = new FlightSegment(id, dist, dur, alt);
        assertEquals(id, seg.identity());
        assertEquals(dist, seg.distance());
        assertEquals(dur, seg.duration());
        assertEquals(alt, seg.altitude());
    }

    @Test
    public void sameAsShouldMatchByIdentity() {
        final FlightSegmentId id = FlightSegmentId.newId();
        final FlightSegment seg1 = new FlightSegment(id, null, null, null);
        final FlightSegment seg2 = new FlightSegment(id, null, null, null);
        assertTrue(seg1.sameAs(seg2));
    }

    @Test
    public void sameAsShouldRejectDifferentId() {
        final FlightSegment seg1 = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        final FlightSegment seg2 = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        assertFalse(seg1.sameAs(seg2));
    }

    @Test
    public void updateWeatherDataShouldSetWind() {
        final FlightSegment seg = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        final FlightWind wind = new FlightWind(
                new WindSpeed(10.0, "KNOT"),
                new WindDirection(180.0),
                MeasurementUnit.KILOGRAM);
        seg.updateWeatherData(wind);
        assertNotNull(seg.wind());
    }
}
