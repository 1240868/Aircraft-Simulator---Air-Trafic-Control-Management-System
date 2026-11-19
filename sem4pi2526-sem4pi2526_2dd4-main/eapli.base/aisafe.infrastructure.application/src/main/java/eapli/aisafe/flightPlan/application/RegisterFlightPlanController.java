package eapli.aisafe.flightPlan.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.flightPlan.domain.DepartureDateTime;
import eapli.aisafe.flightPlan.domain.FuelPlan;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightLegId;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanDesignator;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightSegmentId;
import eapli.aisafe.flightPlan.domain.FlightType;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.flightPlan.domain.FlightWind;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.framework.application.UseCaseController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@UseCaseController
public class RegisterFlightPlanController {

    private final AuthorizationPort authz;
    private final FlightPlanRepository flightPlanRepo;
    private final FlightRouteRepository flightRouteRepo;
    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository aircraftModelRepo;

    public RegisterFlightPlanController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    public RegisterFlightPlanController(final AuthorizationPort authz,
                                        final FlightPlanRepository flightPlanRepo,
                                        final FlightRouteRepository flightRouteRepo,
                                        final AircraftRepository aircraftRepo,
                                        final PilotRepository pilotRepo,
                                        final AirTransportCompanyRepository companyRepo,
                                        final AircraftModelRepository aircraftModelRepo) {
        this.authz = authz;
        this.flightPlanRepo = flightPlanRepo;
        this.flightRouteRepo = flightRouteRepo;
        this.aircraftRepo = aircraftRepo;
        this.pilotRepo = pilotRepo;
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
        final List<Pilot> activePilots = new ArrayList<>();
        for (final Pilot pilot : pilotRepo.findAll()) {
            if (pilot.getStatus() == PilotStatus.ACTIVE) {
                activePilots.add(pilot);
            }
        }
        return activePilots;
    }

    public FlightPlan register(final FlightRouteId flightRouteId,
                               final RegistrationNumber aircraftReg,
                               final PilotId pilotId,
                               final String designator,
                               final String flightTypeStr,
                               final LocalDateTime departureDateTime) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT, Roles.ADMIN);

        final var dto = new FlightPlanDTO(
                flightRouteId.toString(),
                aircraftReg.toString(),
                pilotId.toString(),
                designator,
                flightTypeStr,
                departureDateTime,
                java.util.List.of(),
                null);

        final var factory = new FlightPlanFactory(
                aircraftModelRepo, aircraftRepo, flightPlanRepo,
                companyRepo, pilotRepo, flightRouteRepo);

        if (!factory.validatePreconditions(dto)) {
            throw new IllegalArgumentException("Flight plan validation failed");
        }

        final var id = FlightPlanId.newId();
        final var fpDesignator = new FlightPlanDesignator(designator);
        final var fpType = FlightType.valueOf(flightTypeStr.strip().toUpperCase());
        final var departure = new DepartureDateTime(departureDateTime);

        final var route = flightRouteRepo.findById(flightRouteId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Flight route not found"));
        final var leg = new FlightLeg(
                FlightLegId.newId(),
                route.originAirportId(),
                route.destinationAirportId(),
                1,
                List.of(FlightSegmentId.newId()),
                new FuelPlan(1, 0, MeasurementUnit.KILOGRAM));

        final var wind = new FlightWind(
                new WindSpeed(0, "m/s"),
                new WindDirection(0),
                MeasurementUnit.KILOGRAM);
        final var flightPlan = new FlightPlan(
                id, aircraftReg, flightRouteId, fpDesignator,
                fpType, departure, List.of(leg),
                wind);
        flightPlan.setPilot(pilotId);

        return flightPlanRepo.save(flightPlan);
    }
}
