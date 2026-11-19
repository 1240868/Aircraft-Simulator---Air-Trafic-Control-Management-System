package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

import org.junit.Test;

import java.util.List;

public class FlightLegTest {

    private static FuelPlan fuel() {
        return new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM);
    }

    @Test
    public void shouldCreateValidLeg() {
        final AirportId dep = AirportId.newId();
        final AirportId arr = AirportId.newId();
        final FlightLeg leg = new FlightLeg(FlightLegId.newId(), dep, arr, 1,
                List.of(FlightSegmentId.newId()), fuel());
        assertEquals(dep, leg.departureAirport());
        assertEquals(arr, leg.arrivalAirport());
        assertEquals(1, leg.legOrder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullDeparture() {
        new FlightLeg(FlightLegId.newId(), null, AirportId.newId(), 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullArrival() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), null, 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroLegOrder() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 0,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeLegOrder() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), -1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptySegments() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 1,
                List.of(), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSameAirport() {
        final AirportId same = AirportId.newId();
        new FlightLeg(FlightLegId.newId(), same, same, 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test
    public void shouldStoreFuelPlan() {
        final FuelPlan fp = new FuelPlan(5000, 1000, MeasurementUnit.KILOGRAM);
        final FlightLeg leg = new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 1,
                List.of(FlightSegmentId.newId()), fp);
        assertEquals(fp, leg.fuelPlan());
    }
}
