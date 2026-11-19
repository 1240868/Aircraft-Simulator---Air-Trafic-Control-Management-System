package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.aisafe.weather.domain.WeatherData;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class FlightPlanInsertWeatherDataTest {

    private FlightPlan flightPlan;
    private FlightLeg leg;
    private FlightLegId legId;
    private PilotId pilotId;

    @Before
    public void setUp() {
        final AirportId dep = AirportId.newId();
        final AirportId arr = AirportId.newId();
        legId = FlightLegId.newId();
        leg = new FlightLeg(legId, dep, arr, 1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));

        pilotId = PilotId.newId();
        flightPlan = new FlightPlan(
                FlightPlanId.newId(),
                RegistrationNumber.valueOf("CS-TTK"),
                FlightRouteId.newId(),
                new FlightPlanDesignator("FP001"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30)),
                List.of(leg),
                new FlightWind(new WindSpeed(10, "m/s"), new WindDirection(180), MeasurementUnit.KILOGRAM));
        flightPlan.setPilot(pilotId);
    }

    private WeatherData makeWeather() {
        return new WeatherData(
                new AirControlAreaCode("LPPC"),
                Instant.now(),
                12.5, 90.0, 20.0, 55.0);
    }

    @Test
    public void shouldInsertWeatherDataOnDraftFlightPlan() {
        final WeatherData wd = makeWeather();
        flightPlan.insertWeatherData(wd, legId);

        assertNotNull(leg.weatherData());
        assertEquals(12.5, leg.weatherData().windSpeedMs(), 1e-9);
        assertEquals(90.0, leg.weatherData().windDirectionDegrees(), 1e-9);
    }

    @Test
    public void shouldInsertWeatherDataOnActiveFlightPlan() {
        flightPlan.setStatus(FlightPlanStatus.ACTIVE);
        final WeatherData wd = makeWeather();
        flightPlan.insertWeatherData(wd, legId);

        assertNotNull(leg.weatherData());
        assertEquals(12.5, leg.weatherData().windSpeedMs(), 1e-9);
    }

    @Test
    public void shouldRevertToPendingTestWhenTested() {
        flightPlan.setStatus(FlightPlanStatus.TESTED);
        final WeatherData wd = makeWeather();
        flightPlan.insertWeatherData(wd, legId);

        assertEquals(FlightPlanStatus.PENDING_TEST, flightPlan.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWeatherDataOnCancelledFlightPlan() {
        flightPlan.setStatus(FlightPlanStatus.CANCELLED);
        flightPlan.insertWeatherData(makeWeather(), legId);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWeatherDataOnCompletedFlightPlan() {
        flightPlan.setStatus(FlightPlanStatus.COMPLETED);
        flightPlan.insertWeatherData(makeWeather(), legId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectWeatherDataForInvalidLegId() {
        final FlightLegId unknownLeg = FlightLegId.newId();
        flightPlan.insertWeatherData(makeWeather(), unknownLeg);
    }
}
