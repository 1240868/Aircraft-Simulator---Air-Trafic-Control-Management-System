package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    private FlightPlan createSample() {
        final FlightPlanId id = FlightPlanId.newId();
        final RegistrationNumber aircraft = RegistrationNumber.valueOf("CS-TTK");
        final FlightRouteId flightRoute = FlightRouteId.newId();
        final FlightPlanDesignator designator = new FlightPlanDesignator("FP001");
        final DepartureDateTime departure = new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30));
        final FlightLeg leg = new FlightLeg(FlightLegId.newId(),
                AirportId.forIcaoCode("LPPT"),
                AirportId.forIcaoCode("LPMA"),
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));
        final FlightWind wind = new FlightWind(
                new WindSpeed(10, "m/s"),
                new WindDirection(180),
                MeasurementUnit.KILOGRAM);
        return new FlightPlan(id, aircraft, flightRoute, designator,
                FlightType.COMMERCIAL, departure, List.of(leg), wind);
    }

    @Test
    void testToString() {
        final FlightPlan fp = createSample();
        assertNotNull(fp.toString());
        assertTrue(fp.toString().contains(fp.getDesignator().toString()));
    }

    @Test
    void testEquals() {
        final FlightPlan fp1 = createSample();
        final FlightPlan fp2 = createSample();
        assertEquals(fp1, fp1);
        assertNotEquals(fp1, fp2);
        assertNotEquals(fp1, null);
        assertNotEquals(fp1, "string");
    }

    @Test
    void testHashCode() {
        final FlightPlan fp = createSample();
        final int hc = fp.hashCode();
        assertEquals(hc, fp.hashCode());
    }

    @Test
    void sameAs() {
        final FlightPlan fp1 = createSample();
        final FlightPlan fp2 = createSample();
        final FlightPlan fp3 = new FlightPlan(
                fp1.identity(), fp1.getAircraft(), fp1.getFlightRoute(),
                fp1.getDesignator(), fp1.getFlightType(),
                fp1.getDepartureDateTime(), fp1.getLegs(), fp1.getWind());
        assertFalse(fp1.sameAs(fp1));
        assertTrue(fp1.sameAs(fp3));
        assertFalse(fp1.sameAs(fp2));
        assertFalse(fp1.sameAs(null));
        assertFalse(fp1.sameAs("string"));
    }

    @Test
    void identity() {
        final FlightPlan fp = createSample();
        assertNotNull(fp.identity());
        assertEquals(fp.getId(), fp.identity());
    }

    @Test
    void compareTo() {
        final FlightPlan fp1 = createSample();
        final FlightPlan fp2 = createSample();
        assertEquals(fp1.identity().compareTo(fp2.identity()), fp1.compareTo(fp2.identity()));
    }

    @Test
    void getId() {
        assertNotNull(createSample().getId());
    }

    @Test
    void setId() {
        final FlightPlan fp = createSample();
        final FlightPlanId newId = FlightPlanId.newId();
        fp.setId(newId);
        assertEquals(newId, fp.getId());
    }

    @Test
    void getAircraft() {
        assertNotNull(createSample().getAircraft());
    }

    @Test
    void setAircraft() {
        final FlightPlan fp = createSample();
        final RegistrationNumber reg = RegistrationNumber.valueOf("CS-ABC");
        fp.setAircraft(reg);
        assertEquals(reg, fp.getAircraft());
    }

    @Test
    void getFlightRoute() {
        assertNotNull(createSample().getFlightRoute());
    }

    @Test
    void setFlightRoute() {
        final FlightPlan fp = createSample();
        final FlightRouteId newRoute = FlightRouteId.newId();
        fp.setFlightRoute(newRoute);
        assertEquals(newRoute, fp.getFlightRoute());
    }

    @Test
    void getDesignator() {
        final FlightPlan fp = createSample();
        assertEquals("FP001", fp.getDesignator().value());
    }

    @Test
    void setDesignator() {
        final FlightPlan fp = createSample();
        final FlightPlanDesignator d = new FlightPlanDesignator("FP999");
        fp.setDesignator(d);
        assertEquals(d, fp.getDesignator());
    }

    @Test
    void getStatus() {
        assertEquals(FlightPlanStatus.DRAFT, createSample().getStatus());
    }

    @Test
    void setStatus() {
        final FlightPlan fp = createSample();
        fp.setStatus(FlightPlanStatus.SUBMITTED);
        assertEquals(FlightPlanStatus.SUBMITTED, fp.getStatus());
    }

    @Test
    void getFlightType() {
        assertEquals(FlightType.COMMERCIAL, createSample().getFlightType());
    }

    @Test
    void setFlightType() {
        final FlightPlan fp = createSample();
        fp.setFlightType(FlightType.CARGO);
        assertEquals(FlightType.CARGO, fp.getFlightType());
    }

    @Test
    void getDepartureDateTime() {
        final FlightPlan fp = createSample();
        assertEquals(LocalDateTime.of(2026, 6, 10, 14, 30),
                fp.getDepartureDateTime().value());
    }

    @Test
    void setDepartureDateTime() {
        final FlightPlan fp = createSample();
        final DepartureDateTime dt = new DepartureDateTime(LocalDateTime.of(2026, 7, 4, 8, 0));
        fp.setDepartureDateTime(dt);
        assertEquals(dt, fp.getDepartureDateTime());
    }

    @Test
    void getLegs() {
        assertEquals(1, createSample().getLegs().size());
    }

    @Test
    void setLegs() {
        final FlightPlan fp = createSample();
        final FlightLeg newLeg = new FlightLeg(FlightLegId.newId(),
                AirportId.forIcaoCode("LPPT"),
                AirportId.forIcaoCode("LPMA"),
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(500, 100, MeasurementUnit.KILOGRAM));
        fp.setLegs(List.of(newLeg));
        assertEquals(1, fp.getLegs().size());
        assertEquals(newLeg.identity(), fp.getLegs().get(0).identity());
    }

    @Test
    void getWind() {
        assertNotNull(createSample().getWind());
    }

    @Test
    void setWind() {
        final FlightPlan fp = createSample();
        final FlightWind wind = new FlightWind(
                new WindSpeed(15, "m/s"),
                new WindDirection(270),
                MeasurementUnit.KILOGRAM);
        fp.setWind(wind);
        assertEquals(15.0, fp.getWind().speed().value(), 1e-9);
    }

    @Test
    void updateWeatherData() {
        final FlightPlan fp = createSample();
        fp.updateWeatherData(new WindSpeed(20, "m/s"), new WindDirection(90));
        assertEquals(20.0, fp.getWind().speed().value(), 1e-9);
    }
}
