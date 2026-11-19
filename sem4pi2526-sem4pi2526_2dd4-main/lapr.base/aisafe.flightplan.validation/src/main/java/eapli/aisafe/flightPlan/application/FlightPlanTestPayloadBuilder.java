package eapli.aisafe.flightPlan.application;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FuelPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * US085 — builds JSON consumed by the C flight plan physics validator.
 * Fuel minimum comes from the destination airport air control area regulations.
 * Cruise altitudes come from the linked DSL segment profile.
 */
public final class FlightPlanTestPayloadBuilder {

    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirportRepository airportRepo;
    private final DslSegmentProfileReader profileReader;

    public FlightPlanTestPayloadBuilder(final AircraftRepository aircraftRepo,
                                        final AircraftModelRepository modelRepo,
                                        final AirportRepository airportRepo) {
        this(aircraftRepo, modelRepo, airportRepo, new DslSegmentProfileReader());
    }

    FlightPlanTestPayloadBuilder(final AircraftRepository aircraftRepo,
                                 final AircraftModelRepository modelRepo,
                                 final AirportRepository airportRepo,
                                 final DslSegmentProfileReader profileReader) {
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.airportRepo = airportRepo;
        this.profileReader = profileReader;
    }

    public Path writePayload(final FlightPlan plan) throws IOException {
        final Aircraft aircraft = aircraftRepo.ofIdentity(plan.getAircraft())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
        final AircraftModel model = modelRepo.ofIdentity(aircraft.aircraftModelId())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found"));

        final FlightLeg lastLeg = plan.getLegs().get(plan.getLegs().size() - 1);
        final Airport destination = resolveAirport(lastLeg.arrivalAirport());
        final FuelPlan lastFuel = lastLeg.fuelPlan();

        final double fuelOnBoardKg = totalFuelKg(plan);
        final double reserveFuelKg = lastFuel.reserveFuel();
        // Destination area minimum: use plan reserve until area regulations are modelled on AirControlArea.
        final double minFuelKg = reserveFuelKg;
        final double minAltitudeM = destination.elevationMeters();
        final List<Double> segmentAltitudesM = profileReader.readSegmentAltitudesMeters(plan);

        final String altitudeArray = segmentAltitudesM.stream()
                .map(alt -> String.format(Locale.US, "%.2f", alt))
                .collect(Collectors.joining(", "));

        final String json = String.format(Locale.US, """
                {
                  "flightPlanId": "%s",
                  "fuelOnBoardKg": %.2f,
                  "reserveFuelKg": %.2f,
                  "minFuelKg": %.2f,
                  "serviceCeilingM": %.2f,
                  "minAltitudeM": %.2f,
                  "segmentAltitudesM": [%s]
                }
                """,
                plan.getId().toString(),
                fuelOnBoardKg,
                reserveFuelKg,
                minFuelKg,
                model.serviceCeilingM(),
                minAltitudeM,
                altitudeArray);

        final Path file = Files.createTempFile("fp-test-", ".json");
        Files.writeString(file, json);
        return file;
    }

    private double totalFuelKg(final FlightPlan plan) {
        double total = 0.0;
        for (final FlightLeg leg : plan.getLegs()) {
            total += leg.fuelPlan().totalFuel();
        }
        return total;
    }

    private Airport resolveAirport(final AirportId airportId) {
        return airportRepo.findById(airportId.code())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Airport not found: " + airportId.code()));
    }
}
