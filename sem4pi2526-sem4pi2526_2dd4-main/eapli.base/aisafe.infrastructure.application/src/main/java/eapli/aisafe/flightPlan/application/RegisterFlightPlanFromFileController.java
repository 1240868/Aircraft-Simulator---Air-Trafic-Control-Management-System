package eapli.aisafe.flightPlan.application;

import app.FlightPlanImporter;
import app.ImportResult;
import domain.Fuel;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
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
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.framework.application.UseCaseController;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UseCaseController
public class RegisterFlightPlanFromFileController {

    private static final double JET_A1_KG_PER_LITRE = 0.804;

    private final AuthorizationPort authz;
    private final FlightPlanRepository flightPlanRepo;
    private final FlightRouteRepository flightRouteRepo;
    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;
    private final AirportRepository airportRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository aircraftModelRepo;

    public RegisterFlightPlanFromFileController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    public RegisterFlightPlanFromFileController(final AuthorizationPort authz,
                                                final FlightPlanRepository flightPlanRepo,
                                                final FlightRouteRepository flightRouteRepo,
                                                final AircraftRepository aircraftRepo,
                                                final PilotRepository pilotRepo,
                                                final AirportRepository airportRepo,
                                                final AirTransportCompanyRepository companyRepo,
                                                final AircraftModelRepository aircraftModelRepo) {
        this.authz = authz;
        this.flightPlanRepo = flightPlanRepo;
        this.flightRouteRepo = flightRouteRepo;
        this.aircraftRepo = aircraftRepo;
        this.pilotRepo = pilotRepo;
        this.airportRepo = airportRepo;
        this.companyRepo = companyRepo;
        this.aircraftModelRepo = aircraftModelRepo;
    }

    public Iterable<FlightRoute> listFlightRoutes() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);
        return flightRouteRepo.findAll();
    }

    public Iterable<Aircraft> listAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);
        return aircraftRepo.findAll();
    }

    public Iterable<Pilot> listPilots() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);
        return pilotRepo.findAll();
    }

    /**
     * Parses, validates and registers a flight plan from a DSL file.
     *
     * @param filePath   path to the .dfdsl file
     * @param routeId    system flight route to associate
     * @param aircraftReg aircraft registration number
     * @param pilotId    pilot identity
     * @return the persisted FlightPlan
     * @throws IllegalArgumentException if the DSL file is invalid or a referenced
     *                                  airport is not registered in the system
     * @throws IOException              if the file cannot be read
     */
    public FlightPlan registerFromFile(final Path filePath,
                                       final FlightRouteId routeId,
                                       final RegistrationNumber aircraftReg,
                                       final PilotId pilotId) throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);

        final ImportResult result = new FlightPlanImporter().importFromFile(filePath);

        if (!result.isValid()) {
            final String syntaxMsg = result.syntaxErrors().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            final String semanticMsg = result.semanticErrors().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new IllegalArgumentException(
                    "Invalid flight plan file:\n" + syntaxMsg + semanticMsg);
        }

        final domain.FlightPlan dslPlan = result.flightPlan();

        validateBusinessPreconditions(dslPlan.flight().id(), routeId, aircraftReg, pilotId);

        final List<FlightLeg> legs = buildLegs(dslPlan);

        final domain.Leg firstDslLeg = dslPlan.flight().legs().get(0);
        final LocalDateTime departureDateTime = LocalDateTime.of(
                firstDslLeg.departureDate(), firstDslLeg.departureTime());

        final FlightPlan flightPlan = new FlightPlan(
                FlightPlanId.newId(),
                aircraftReg,
                routeId,
                new FlightPlanDesignator(dslPlan.flight().id()),
                mapFlightType(dslPlan.flight().type()),
                new DepartureDateTime(departureDateTime),
                legs,
                null);
        flightPlan.setPilot(pilotId);
        flightPlan.setDslSourcePath(filePath.toAbsolutePath().toString());

        return flightPlanRepo.save(flightPlan);
    }

    private void validateBusinessPreconditions(final String designator,
                                               final FlightRouteId routeId,
                                               final RegistrationNumber aircraftReg,
                                               final PilotId pilotId) {
        final FlightPlanDTO dto = new FlightPlanDTO(
                routeId.toString(),
                aircraftReg.toString(),
                pilotId.toString(),
                designator,
                null,
                null,
                List.of(),
                null);

        final FlightPlanFactory factory = new FlightPlanFactory(
                aircraftModelRepo, aircraftRepo, flightPlanRepo,
                companyRepo, pilotRepo, flightRouteRepo);

        if (!factory.validatePreconditions(dto)) {
            throw new IllegalArgumentException("Flight plan business validation failed");
        }
    }

    private List<FlightLeg> buildLegs(final domain.FlightPlan dslPlan) {
        final List<FlightLeg> legs = new ArrayList<>();
        final List<domain.Leg> dslLegs = dslPlan.flight().legs();
        for (int i = 0; i < dslLegs.size(); i++) {
            final domain.Leg dslLeg = dslLegs.get(i);
            final AirportId depId = resolveAirport(dslLeg.departureAirport().code());
            final AirportId arrId = resolveAirport(dslLeg.arrivalAirport().code());

            final List<FlightSegmentId> segmentIds = dslLeg.segments().stream()
                    .map(s -> FlightSegmentId.newId())
                    .collect(Collectors.toList());

            final FuelPlan fuelPlan = buildFuelPlan(dslLeg.fuel());

            legs.add(new FlightLeg(FlightLegId.newId(), depId, arrId, i + 1, segmentIds, fuelPlan));
        }
        return legs;
    }

    private AirportId resolveAirport(final String code) {
        final Airport airport;
        if (code.length() == 4) {
            airport = airportRepo.findByIcaoCode(code)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Airport with ICAO code '" + code + "' is not registered in the system"));
        } else {
            airport = airportRepo.findByIataCode(code)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Airport with IATA code '" + code + "' is not registered in the system"));
        }
        return AirportId.forIcaoCode(airport.icaoCode());
    }

    private FuelPlan buildFuelPlan(final Fuel fuel) {
        final double kg;
        final String unit = fuel.unit().toLowerCase();
        if (unit.equals("l")) {
            kg = fuel.quantity() * JET_A1_KG_PER_LITRE;
        } else {
            kg = fuel.quantity();
        }
        return new FuelPlan(kg, 0, MeasurementUnit.KILOGRAM);
    }

    private FlightType mapFlightType(final domain.FlightType dslType) {
        return switch (dslType) {
            case REGULAR -> FlightType.COMMERCIAL;
            case CHARTER -> FlightType.CHARTER;
        };
    }
}
