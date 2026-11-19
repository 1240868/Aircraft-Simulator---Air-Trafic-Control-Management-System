package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.flightPlan.domain.DepartureDateTime;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightLegId;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanDesignator;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightSegmentId;
import eapli.aisafe.flightPlan.domain.FlightType;
import eapli.aisafe.flightPlan.domain.FlightWind;
import eapli.aisafe.flightPlan.domain.FuelPlan;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Test;

public class FlightPlanDslFileValidatorTest {

    private static final String VALID_DSL = """
            flight AA101 type regular route TP001 {
                leg {
                    departure LIS 2026-05-01 08:00
                    arrival MAD 2026-05-01 10:00
                    segment {
                        start 38.7742, -9.1342
                        end 40.4168, -3.7038
                        altitude 35000 m width 10 m
                        wind 270 deg, 20 m/s
                    }
                    fuel 3000 kg
                }
            }
            """;

    private final FlightPlanDslFileValidator validator = new FlightPlanDslFileValidator();

    @Test
    public void shouldAcceptValidDslFile() throws Exception {
        final Path file = Files.createTempFile("valid-plan-", ".dfdsl");
        Files.writeString(file, VALID_DSL);

        final FlightPlan plan = samplePlan();
        plan.setDslSourcePath(file.toString());

        assertTrue(validator.validate(plan).isEmpty());
    }

    @Test
    public void shouldRejectMissingDslSource() {
        final FlightPlan plan = samplePlan();
        final List<String> errors = validator.validate(plan);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("No DSL source"));
    }

    @Test
    public void shouldRejectInvalidDslSyntax() throws Exception {
        final Path file = Files.createTempFile("invalid-plan-", ".dfdsl");
        Files.writeString(file, "flight broken {");

        final FlightPlan plan = samplePlan();
        plan.setDslSourcePath(file.toString());

        final List<String> errors = validator.validate(plan);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Syntax error")));
    }

    private static FlightPlan samplePlan() {
        final FlightLeg leg = new FlightLeg(
                FlightLegId.newId(),
                AirportId.newId(),
                AirportId.newId(),
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));

        return new FlightPlan(
                FlightPlanId.newId(),
                RegistrationNumber.valueOf("CS-TTK"),
                FlightRouteId.newId(),
                new FlightPlanDesignator("FP001"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30)),
                List.of(leg),
                new FlightWind(new WindSpeed(10, "m/s"), new WindDirection(180), MeasurementUnit.KILOGRAM));
    }
}
