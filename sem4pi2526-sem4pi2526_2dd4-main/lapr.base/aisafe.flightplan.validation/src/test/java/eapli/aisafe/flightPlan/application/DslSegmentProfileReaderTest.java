package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightPlan.domain.DepartureDateTime;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightLegId;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanDesignator;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightSegmentId;
import eapli.aisafe.flightPlan.domain.FlightType;
import eapli.aisafe.flightPlan.domain.FuelPlan;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Test;

public class DslSegmentProfileReaderTest {

    @Test
    public void shouldReadSegmentAltitudesFromDslFile() throws Exception {
        final String dsl = """
                flight AA101 type regular route TP001 {
                    leg {
                        departure LIS 2026-05-01 08:00
                        arrival MAD 2026-05-01 10:00
                        segment {
                            start 38.7742, -9.1342
                            end 40.4168, -3.7038
                            altitude 35000 ft width 10 m
                            wind 270 deg, 20 m/s
                        }
                        fuel 3000 kg
                    }
                }
                """;
        final Path file = Files.createTempFile("profile-", ".dfdsl");
        Files.writeString(file, dsl);

        final FlightPlan plan = new FlightPlan(
                FlightPlanId.newId(),
                RegistrationNumber.valueOf("CS-TTK"),
                FlightRouteId.newId(),
                new FlightPlanDesignator("AA101"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 5, 1, 8, 0)),
                List.of(new FlightLeg(
                        FlightLegId.newId(),
                        AirportId.forIcaoCode("LPPT"),
                        AirportId.forIcaoCode("LEMD"),
                        1,
                        List.of(FlightSegmentId.newId()),
                        new FuelPlan(3000, 0, MeasurementUnit.KILOGRAM))),
                null);
        plan.setDslSourcePath(file.toString());

        final List<Double> altitudes = new DslSegmentProfileReader().readSegmentAltitudesMeters(plan);

        assertEquals(1, altitudes.size());
        assertEquals(35000 * 0.3048, altitudes.get(0), 0.1);
    }
}
