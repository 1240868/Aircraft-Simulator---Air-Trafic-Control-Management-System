package eapli.aisafe.flightPlan.application;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightLegId;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UseCaseController
public class InsertWeatherDataInFlightController {

    private final AuthorizationPort authz;
    private final FlightPlanRepository flightPlanRepo;
    private final WeatherDataRepository weatherDataRepo;
    private final FlightRouteRepository flightRouteRepo;
    private final AirportRepository airportRepo;
    private final AirControlAreaRepository airControlAreaRepo;
    private final PilotRepository pilotRepo;

    public InsertWeatherDataInFlightController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().pilots());
    }

    public InsertWeatherDataInFlightController(final AuthorizationPort authz,
                                                final FlightPlanRepository flightPlanRepo,
                                                final WeatherDataRepository weatherDataRepo,
                                                final FlightRouteRepository flightRouteRepo,
                                                final AirportRepository airportRepo,
                                                final AirControlAreaRepository airControlAreaRepo,
                                                final PilotRepository pilotRepo) {
        this.authz = authz;
        this.flightPlanRepo = flightPlanRepo;
        this.weatherDataRepo = weatherDataRepo;
        this.flightRouteRepo = flightRouteRepo;
        this.airportRepo = airportRepo;
        this.airControlAreaRepo = airControlAreaRepo;
        this.pilotRepo = pilotRepo;
    }

    public List<FlightPlan> listPlansForCurrentPilot() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);
        final PilotId pilotId = resolvePilotIdForCurrentUser();
        final List<FlightPlan> plans = new ArrayList<>();
        for (final FlightPlan plan : flightPlanRepo.findAll()) {
            if (pilotId.equals(plan.getPilot())
                    && plan.getStatus() != FlightPlanStatus.CANCELLED
                    && plan.getStatus() != FlightPlanStatus.COMPLETED) {
                plans.add(plan);
            }
        }
        plans.sort(Comparator.comparing(fp -> fp.getDesignator().toString()));
        return plans;
    }

    public List<FlightLeg> listLegsForPlan(final FlightPlanId flightPlanId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);
        final PilotId pilotId = resolvePilotIdForCurrentUser();
        final FlightPlan plan = flightPlanRepo.findById(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Flight plan not found: " + flightPlanId));
        if (!pilotId.equals(plan.getPilot())) {
            throw new IllegalArgumentException("Flight plan does not belong to the authenticated pilot");
        }
        return plan.getLegs();
    }

    public void insertWeatherData(final InsertWeatherDataDTO dto) {
        insertWeatherData(dto, resolvePilotIdForCurrentUser());
    }

    public void insertWeatherData(final InsertWeatherDataDTO dto, final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);

        final var flightPlanId = new FlightPlanId(UUID.fromString(dto.flightPlanId()));
        final var flightPlan = flightPlanRepo.findById(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Flight plan not found: " + dto.flightPlanId()));

        if (!pilotId.equals(flightPlan.getPilot())) {
            throw new IllegalArgumentException("Flight plan does not belong to the authenticated pilot");
        }

        final var areaCode = new AirControlAreaCode(dto.areaCode());

        validateCoherence(flightPlan, areaCode, dto.observedAt());

        final var weatherData = new WeatherData(
                areaCode,
                dto.observedAt(),
                dto.windSpeedMs(),
                dto.windDirectionDeg(),
                dto.temperatureC(),
                dto.humidityPercent());

        final var savedWeatherData = weatherDataRepo.save(weatherData);

        final var legId = new FlightLegId(UUID.fromString(dto.legId()));

        flightPlan.insertWeatherData(savedWeatherData, legId);

        flightPlanRepo.save(flightPlan);
    }

    private void validateCoherence(final FlightPlan flightPlan,
                                    final AirControlAreaCode areaCode,
                                    final java.time.Instant observedAt) {
        if (airControlAreaRepo.findByCode(areaCode).isEmpty()) {
            throw new IllegalArgumentException(
                    "Air control area not found: " + areaCode);
        }

        final FlightRoute route = flightRouteRepo.ofIdentity(flightPlan.getFlightRoute())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight route not found for flight plan"));

        boolean areaMatchesRoute = matchesAnyAirportArea(route.originAirportId(), areaCode)
                || matchesAnyAirportArea(route.destinationAirportId(), areaCode);
        for (final FlightLeg leg : flightPlan.getLegs()) {
            if (matchesAnyAirportArea(leg.departureAirport(), areaCode)
                    || matchesAnyAirportArea(leg.arrivalAirport(), areaCode)) {
                areaMatchesRoute = true;
                break;
            }
        }

        if (!areaMatchesRoute) {
            throw new IllegalArgumentException(
                    "Weather data area " + areaCode
                            + " does not match any airport area on the flight plan route");
        }

        final LocalDateTime departure = flightPlan.getDepartureDateTime().value();
        final LocalDate departureDate = departure.toLocalDate();
        final LocalDate observedDate = observedAt.atZone(ZoneId.systemDefault()).toLocalDate();

        if (observedDate.isBefore(departureDate.minusDays(1))
                || observedDate.isAfter(departureDate.plusDays(1))) {
            throw new IllegalArgumentException(
                    "Weather data date " + observedDate
                            + " is not within 1 day of flight departure " + departureDate);
        }
    }

    private boolean matchesAnyAirportArea(final AirportId airportId,
                                           final AirControlAreaCode areaCode) {
        return airportRepo.findById(airportId.code())
                .filter(airport -> airport.airControlArea() != null)
                .map(airport -> airport.airControlArea().identity().equals(areaCode))
                .orElse(false);
    }

    private PilotId resolvePilotIdForCurrentUser() {
        final String username = authz.currentUsername().strip();
        Optional<Pilot> pilot = emailFromAuthenticatedSession()
                .flatMap(email -> pilotRepo.findByEmail(email));
        if (pilot.isEmpty() && looksLikeEmail(username)) {
            pilot = pilotRepo.findByEmail(new Email(username));
        }
        return pilot.orElseThrow(() -> new IllegalArgumentException(
                        "No pilot profile linked to user: " + username))
                .identity();
    }

    private Optional<Email> emailFromAuthenticatedSession() {
        try {
            return AuthzRegistry.authorizationService().session()
                    .flatMap(session -> {
                        final var user = session.authenticatedUser();
                        if (user.email() == null || user.email().toString().isBlank()) {
                            return Optional.empty();
                        }
                        return Optional.of(new Email(user.email().toString().strip()));
                    });
        } catch (final IllegalStateException ex) {
            return Optional.empty();
        }
    }

    private static boolean looksLikeEmail(final String value) {
        return value.contains("@");
    }
}
