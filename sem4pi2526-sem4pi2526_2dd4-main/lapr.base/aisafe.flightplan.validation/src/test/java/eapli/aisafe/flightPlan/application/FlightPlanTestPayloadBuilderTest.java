package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class FlightPlanTestPayloadBuilderTest {

    private static final String VALID_DSL = """
            flight AA101 type regular route TP001 {
                leg {
                    departure LIS 2026-05-01 08:00
                    arrival MAD 2026-05-01 10:00
                    segment {
                        start 38.7742, -9.1342
                        end 40.4168, -3.7038
                        altitude 10000 m width 10 m
                        wind 270 deg, 20 m/s
                    }
                    fuel 3000 kg
                }
            }
            """;

    @Test
    public void shouldIncludeReserveFuelAsMinFuelAndDslAltitudes() throws Exception {
        final Path dslFile = Files.createTempFile("plan-", ".dfdsl");
        Files.writeString(dslFile, VALID_DSL);

        final RegistrationNumber reg = RegistrationNumber.valueOf("CS-TTK");
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(new EngineModelId(UUID.randomUUID())));
        final AircraftModelId modelId = model.identity();
        final CompanyId companyId = new CompanyId(UUID.randomUUID());
        final Aircraft aircraft = AircraftFactory.registerNew(
                "CS-TTK", "PT", modelId, companyId,
                List.of(new CabinSeatBlock("Economy", 180)),
                200, LocalDate.of(2015, 1, 1), LocalDate.of(2015, 6, 1));

        final AirportId mad = AirportId.forIcaoCode("LEMD");
        final AirControlArea area = new AirControlArea("ES-CTR", "Madrid", 36, 44, -10, 5);
        final Airport destination = new Airport(
                "LEMD", "MAD", "Madrid", "Madrid", "ES",
                40.4168, -3.7038, 610.0, area);

        final FlightPlan plan = samplePlan(reg, mad, dslFile.toString());

        final FlightPlanTestPayloadBuilder builder = new FlightPlanTestPayloadBuilder(
                aircraftRepo(reg, aircraft),
                modelRepo(modelId, model),
                airportRepo(mad, destination));

        final String json = Files.readString(builder.writePayload(plan));

        assertTrue(json.contains("\"minFuelKg\": 500.00"));
        assertTrue(json.contains("\"segmentAltitudesM\": [10000.00]"));
    }

    @Test
    public void shouldConvertFeetToMetersInProfileReader() {
        assertTrue(Math.abs(DslSegmentProfileReader.toMeters(10000, "ft") - 3048.0) < 0.1);
    }

    private static FlightPlan samplePlan(final RegistrationNumber reg,
                                         final AirportId arrival,
                                         final String dslPath) {
        final FlightLeg leg = new FlightLeg(
                FlightLegId.newId(),
                AirportId.forIcaoCode("LPPT"),
                arrival,
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(3000, 500, MeasurementUnit.KILOGRAM));

        final FlightPlan plan = new FlightPlan(
                FlightPlanId.newId(),
                reg,
                FlightRouteId.newId(),
                new FlightPlanDesignator("AA101"),
                FlightType.COMMERCIAL,
                new DepartureDateTime(LocalDateTime.of(2026, 5, 1, 8, 0)),
                List.of(leg),
                null);
        plan.setDslSourcePath(dslPath);
        return plan;
    }

    private static AircraftRepository aircraftRepo(final RegistrationNumber reg, final Aircraft aircraft) {
        return new AircraftRepository() {
            @Override
            public Optional<Aircraft> ofIdentity(final RegistrationNumber id) {
                return reg.equals(id) ? Optional.of(aircraft) : Optional.empty();
            }

            @Override
            public Optional<Aircraft> findByRegistration(final String registration) {
                return Optional.empty();
            }

            @Override
            public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
                return reg.equals(registration) ? Optional.of(aircraft) : Optional.empty();
            }

            @Override
            public Aircraft save(final Aircraft entity) { return entity; }

            @Override
            public Iterable<Aircraft> findAll() { return List.of(); }

            @Override
            public void delete(final Aircraft entity) { }

            @Override
            public void deleteOfIdentity(final RegistrationNumber id) { }

            @Override
            public long count() { return 0; }

            @Override
            public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
                return List.of();
            }

            @Override
            public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                                       final AircraftModelId modelId) {
                return List.of();
            }

            @Override
            public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                                   final int passengerCapacity) {
                return List.of();
            }

            @Override
            public Iterable<Aircraft> findActiveByOwnerCompanyAndAge(final CompanyId companyId,
                                                                     final int completeYears,
                                                                     final LocalDate asOf) {
                return List.of();
            }
        };
    }

    private static AircraftModelRepository modelRepo(final AircraftModelId id, final AircraftModel model) {
        return new AircraftModelRepository() {
            @Override
            public Optional<AircraftModel> ofIdentity(final AircraftModelId modelId) {
                return id.equals(modelId) ? Optional.of(model) : Optional.empty();
            }

            @Override
            public Optional<AircraftModel> findById(final String modelId) { return Optional.empty(); }

            @Override
            public Optional<AircraftModel> findById(final AircraftModelId modelId) {
                return id.equals(modelId) ? Optional.of(model) : Optional.empty();
            }

            @Override
            public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                          final String manufacturer) {
                return Optional.empty();
            }

            @Override
            public AircraftModel save(final AircraftModel entity) { return entity; }

            @Override
            public Iterable<AircraftModel> findAll() { return List.of(); }

            @Override
            public void delete(final AircraftModel entity) { }

            @Override
            public void deleteOfIdentity(final AircraftModelId modelId) { }

            @Override
            public long count() { return 0; }
        };
    }

    private static AirportRepository airportRepo(final AirportId id, final Airport airport) {
        return new AirportRepository() {
            @Override
            public Optional<Airport> findByIcaoCode(final String icaoCode) {
                return Optional.of(airport);
            }

            @Override
            public Optional<Airport> findByIataCode(final String iataCode) {
                return Optional.empty();
            }

            @Override
            public Optional<Airport> findById(final String code) {
                return id.code().equals(code) ? Optional.of(airport) : Optional.empty();
            }

            @Override
            public Optional<Airport> ofIdentity(final String code) {
                return findById(code);
            }

            @Override
            public Airport save(final Airport entity) { return entity; }

            @Override
            public Iterable<Airport> findAll() { return List.of(airport); }

            @Override
            public void delete(final Airport entity) { }

            @Override
            public void deleteOfIdentity(final String id) { }

            @Override
            public long count() { return 0; }

            @Override
            public Boolean exists(final AirportId airportId) {
                return id.equals(airportId);
            }
        };
    }
}
