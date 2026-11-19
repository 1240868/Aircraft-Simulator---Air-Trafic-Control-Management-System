package eapli.aisafe.app.pilot.server;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightPlan.application.FlightPlanDslFileValidator;
import eapli.aisafe.flightPlan.application.FlightPlanPhysicsValidator;
import eapli.aisafe.flightPlan.application.FlightPlanTestPayloadBuilder;
import eapli.aisafe.flightPlan.application.InsertWeatherDataDTO;
import eapli.aisafe.flightPlan.application.InsertWeatherDataInFlightController;
import eapli.aisafe.flightPlan.application.RegisterFlightPlanController;
import eapli.aisafe.flightPlan.application.RegisterFlightPlanFromFileController;
import eapli.aisafe.flightPlan.application.TestFlightPlanController;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.weather.application.ConsultWeatherDataController;
import eapli.aisafe.weather.application.consultation.WeatherConsultationResult;
import eapli.aisafe.weather.application.consultation.WeatherDataSummary;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.io.IOException;
import java.lang.reflect.Constructor;
import eapli.aisafe.flightPlan.application.FlightPlanTestResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** TCP command processing for Pilot remote access. */
final class PilotSessionHandler {

    private final String clientIp;
    private final int clientPort;
    private final RemoteAccessUdpLogger accessLogger;

    private UserSession currentSession;
    private String loggedInUsername;
    private boolean disconnectLogged;

    PilotSessionHandler(final String clientIp,
                        final int clientPort,
                        final RemoteAccessUdpLogger accessLogger) {
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.accessLogger = accessLogger;
    }

    String processCommand(final String line) {
        final String[] parts = line.split("\\|", -1);
        final String cmd = parts[0].toUpperCase();

        return switch (cmd) {
            case "LOGIN" -> handleLogin(parts);
            case "LOGOUT" -> handleLogout();
            case "PING" -> "PONG";
            case "LIST_ROUTES" -> handleListRoutes();
            case "LIST_AIRCRAFT" -> handleListAircraft();
            case "LIST_MY_FLIGHT_PLANS" -> handleListMyFlightPlans();
            case "LIST_TESTABLE_PLANS" -> handleListTestablePlans();
            case "REGISTER_FLIGHT_PLAN" -> handleRegisterFlightPlan(parts);
            case "REGISTER_FLIGHT_PLAN_FILE" -> handleRegisterFlightPlanFile(parts);
            case "INSERT_WEATHER" -> handleInsertWeather(parts);
            case "CONSULT_WEATHER" -> handleConsultWeather(parts);
            case "TEST_FLIGHT_PLAN" -> handleTestFlightPlan(parts);
            default -> "ERROR|UNKNOWN_COMMAND";
        };
    }

    void notifyDisconnect() {
        if (disconnectLogged || loggedInUsername == null) {
            return;
        }
        accessLogger.logDisconnect(loggedInUsername, clientIp, clientPort);
        disconnectLogged = true;
    }

    private String handleLogin(final String[] parts) {
        if (parts.length < 3) {
            return "ERROR|INVALID_FORMAT";
        }

        final String username = parts[1];
        final String password = parts[2];

        try {
            final Constructor<AuthorizationService> ctor =
                    AuthorizationService.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            final AuthorizationService localAuthz = ctor.newInstance();

            final AuthenticationService localAuthn = new AuthenticationService(
                    PersistenceContext.repositories().users(),
                    localAuthz,
                    new AisafePasswordPolicy(),
                    new PlainTextEncoder());

            final Optional<UserSession> session =
                    localAuthn.authenticate(username, password, Roles.PILOT);

            if (session.isEmpty()) {
                accessLogger.logLoginFailure(username, clientIp, clientPort);
                return "ERROR|INVALID_CREDENTIALS";
            }

            this.currentSession = session.get();
            this.loggedInUsername = username;
            this.disconnectLogged = false;
            accessLogger.logLoginSuccess(username, clientIp, clientPort);
            return "OK";

        } catch (final Exception e) {
            accessLogger.logLoginFailure(username, clientIp, clientPort);
            return "ERROR|AUTH_FAILED";
        }
    }

    private String handleLogout() {
        if (loggedInUsername != null) {
            accessLogger.logLogout(loggedInUsername, clientIp, clientPort);
            disconnectLogged = true;
        }
        this.currentSession = null;
        this.loggedInUsername = null;
        return "BYE";
    }

    private String handleListRoutes() {
        if (!ensurePilot()) {
            return authError();
        }
        try {
            final var controller = registerFlightPlanController();
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final FlightRoute route : controller.listFlightRoutes()) {
                count++;
            }
            sb.append("OK|").append(count);
            for (final FlightRoute route : controller.listFlightRoutes()) {
                sb.append('\n')
                        .append(route.identity()).append('|')
                        .append(route.getName().value()).append('|')
                        .append(route.originAirportId()).append('|')
                        .append(route.destinationAirportId());
            }
            return sb.toString();
        } catch (final Exception e) {
            return "ERROR|LIST_ROUTES_FAILED";
        }
    }

    private String handleListAircraft() {
        if (!ensurePilot()) {
            return authError();
        }
        try {
            final var controller = registerFlightPlanController();
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final Aircraft aircraft : controller.listAircraft()) {
                count++;
            }
            sb.append("OK|").append(count);
            for (final Aircraft aircraft : controller.listAircraft()) {
                sb.append('\n')
                        .append(aircraft.identity()).append('|')
                        .append(aircraft.aircraftModelId()).append('|')
                        .append(aircraft.operationalStatus());
            }
            return sb.toString();
        } catch (final Exception e) {
            return "ERROR|LIST_AIRCRAFT_FAILED";
        }
    }

    private String handleListMyFlightPlans() {
        if (!ensurePilot()) {
            return authError();
        }
        try {
            final PilotId pilotId = resolvePilotId();
            final List<FlightPlan> plans = new java.util.ArrayList<>();
            for (final FlightPlan fp : PersistenceContext.repositories().flightPlans().findAll()) {
                if (pilotId.equals(fp.getPilot())) {
                    plans.add(fp);
                }
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("OK|").append(plans.size());
            for (final FlightPlan plan : plans) {
                sb.append('\n')
                        .append(plan.identity()).append('|')
                        .append(plan.getDesignator().value()).append('|')
                        .append(plan.getStatus()).append('|')
                        .append(plan.getFlightRoute());
                for (final FlightLeg leg : plan.getLegs()) {
                    sb.append('\n')
                            .append("LEG|")
                            .append(plan.identity()).append('|')
                            .append(leg.identity()).append('|')
                            .append(leg.legOrder());
                }
            }
            return sb.toString();
        } catch (final IllegalArgumentException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|LIST_PLANS_FAILED";
        }
    }

    private String handleListTestablePlans() {
        if (!ensurePilot()) {
            return authError();
        }
        try {
            final var controller = testFlightPlanController();
            final List<FlightPlan> plans = controller.listTestablePlansForCurrentPilot();

            final StringBuilder sb = new StringBuilder();
            sb.append("OK|").append(plans.size());
            for (final FlightPlan plan : plans) {
                sb.append('\n')
                        .append(plan.identity()).append('|')
                        .append(plan.getDesignator().value()).append('|')
                        .append(plan.getStatus());
            }
            return sb.toString();
        } catch (final IllegalArgumentException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|LIST_TESTABLE_FAILED";
        }
    }

    private String handleRegisterFlightPlan(final String[] parts) {
        if (!ensurePilot()) {
            return authError();
        }
        if (parts.length < 7) {
            return "ERROR|INVALID_FORMAT";
        }

        try {
            final FlightRouteId routeId = new FlightRouteId(UUID.fromString(parts[1].trim()));
            final RegistrationNumber aircraftReg = RegistrationNumber.valueOf(parts[2].trim());
            final String designator = parts[3].trim();
            final String flightType = parts[4].trim();
            final LocalDateTime departure = LocalDateTime.parse(parts[5].trim() + "T" + parts[6].trim());

            final PilotId pilotId = resolvePilotId();
            final var controller = registerFlightPlanController();
            final FlightPlan saved = controller.register(
                    routeId, aircraftReg, pilotId, designator, flightType, departure);
            return "OK|" + saved.identity() + '|' + saved.getStatus();

        } catch (final DateTimeParseException e) {
            return "ERROR|INVALID_DATETIME";
        } catch (final IllegalArgumentException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|REGISTER_FAILED";
        }
    }

    private String handleRegisterFlightPlanFile(final String[] parts) {
        if (!ensurePilot()) {
            return authError();
        }
        if (parts.length < 4) {
            return "ERROR|INVALID_FORMAT";
        }

        try {
            final Path filePath = Path.of(parts[1].trim());
            final FlightRouteId routeId = new FlightRouteId(UUID.fromString(parts[2].trim()));
            final RegistrationNumber aircraftReg = RegistrationNumber.valueOf(parts[3].trim());
            final PilotId pilotId = resolvePilotId();

            final var controller = registerFlightPlanFromFileController();
            final FlightPlan saved = controller.registerFromFile(filePath, routeId, aircraftReg, pilotId);
            return "OK|" + saved.identity() + '|' + saved.getDesignator().value() + '|' + saved.getStatus();

        } catch (final IOException e) {
            return "ERROR|FILE_READ_FAILED|" + sanitize(e.getMessage());
        } catch (final IllegalArgumentException e) {
            return "ERROR|DSL_VALIDATION_FAILED|" + sanitizeMultiline(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|REGISTER_FILE_FAILED";
        }
    }

    private String handleInsertWeather(final String[] parts) {
        if (!ensurePilot()) {
            return authError();
        }
        if (parts.length < 9) {
            return "ERROR|INVALID_FORMAT";
        }

        try {
            final Instant observedAt = parseInstant(parts[4]);
            final double temperatureC = Double.parseDouble(parts[5]);
            final double humidityPercent = Double.parseDouble(parts[6]);
            final double windSpeedMs = Double.parseDouble(parts[7]);
            final double windDirectionDeg = Double.parseDouble(parts[8]);

            final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim(),
                    observedAt,
                    windSpeedMs,
                    windDirectionDeg,
                    temperatureC,
                    humidityPercent);

            final var controller = new InsertWeatherDataInFlightController(
                    authorizationPort(), PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().weatherData(),
                    PersistenceContext.repositories().flightRoutes(),
                    PersistenceContext.repositories().airports(),
                    PersistenceContext.repositories().airControlAreas(),
                    PersistenceContext.repositories().pilots());

            controller.insertWeatherData(dto, resolvePilotId());
            return "OK";

        } catch (final NumberFormatException e) {
            return "ERROR|INVALID_NUMBER";
        } catch (final DateTimeParseException e) {
            return "ERROR|INVALID_DATE";
        } catch (final IllegalArgumentException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|INSERT_WEATHER_FAILED";
        }
    }

    private String handleConsultWeather(final String[] parts) {
        if (!ensurePilot()) {
            return authError();
        }
        if (parts.length < 3) {
            return "ERROR|INVALID_FORMAT";
        }

        try {
            final LocalDate date = LocalDate.parse(parts[2].trim());
            final var controller = new ConsultWeatherDataController(
                    authorizationPort(),
                    PersistenceContext.repositories().weatherData(),
                    PersistenceContext.repositories().airControlAreas());

            final WeatherConsultationResult result = controller.consult(parts[1].trim(), date);
            if (result.recordCount() == 0) {
                return "OK|0";
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("OK|").append(result.recordCount());
            for (final WeatherDataSummary r : result.records()) {
                sb.append('\n')
                        .append(r.weatherDataId()).append('|')
                        .append(r.areaCode()).append('|')
                        .append(r.observedAt()).append('|')
                        .append(r.temperatureC()).append('|')
                        .append(r.humidityPercent()).append('|')
                        .append(r.windSpeedMs()).append('|')
                        .append(r.windDirectionDegrees()).append('|')
                        .append(sanitize(r.source()));
            }
            return sb.toString();

        } catch (final DateTimeParseException e) {
            return "ERROR|INVALID_DATE";
        } catch (final IllegalArgumentException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|CONSULT_WEATHER_FAILED";
        }
    }

    private String handleTestFlightPlan(final String[] parts) {
        if (!ensurePilot()) {
            return authError();
        }
        if (parts.length < 2) {
            return "ERROR|INVALID_FORMAT";
        }

        try {
            final FlightPlanId planId = new FlightPlanId(UUID.fromString(parts[1].trim()));
            final var controller = testFlightPlanController();
            final FlightPlanTestResult result = controller.testForCurrentPilot(planId);

            if (result.passed()) {
                return "OK|PASSED|" + result.newStatus();
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("OK|FAILED|").append(result.newStatus());
            for (final String err : result.dslErrors()) {
                sb.append('\n').append("DSL|").append(sanitize(err));
            }
            for (final String err : result.physicsErrors()) {
                sb.append('\n').append("PHYSICS|").append(sanitize(err));
            }
            return sb.toString();

        } catch (final IllegalArgumentException | IllegalStateException e) {
            return "ERROR|" + sanitize(e.getMessage());
        } catch (final Exception e) {
            return "ERROR|TEST_FAILED";
        }
    }

    private RegisterFlightPlanController registerFlightPlanController() {
        return new RegisterFlightPlanController(
                authorizationPort(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    private RegisterFlightPlanFromFileController registerFlightPlanFromFileController() {
        return new RegisterFlightPlanFromFileController(
                authorizationPort(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    private TestFlightPlanController testFlightPlanController() {
        return new TestFlightPlanController(
                authorizationPort(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().pilots(),
                new FlightPlanDslFileValidator(),
                new FlightPlanTestPayloadBuilder(
                        PersistenceContext.repositories().aircraft(),
                        PersistenceContext.repositories().aircraftModels(),
                        PersistenceContext.repositories().airports())::writePayload,
                new FlightPlanPhysicsValidator(Paths.get("lapr.base")));
    }

    private PilotId resolvePilotId() {
        final SystemUser user = currentSession.authenticatedUser();
        final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();

        if (user.email() != null && !user.email().toString().isBlank()) {
            final Optional<Pilot> byEmail = pilotRepo.findByEmail(new Email(user.email().toString().strip()));
            if (byEmail.isPresent()) {
                return byEmail.get().identity();
            }
        }

        final String username = user.username().toString().strip();
        if (username.contains("@")) {
            return pilotRepo.findByEmail(new Email(username))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No pilot profile linked to user: " + username))
                    .identity();
        }

        throw new IllegalArgumentException(
                "No pilot profile linked to user: " + username
                        + ". Ensure the pilot record uses the same email as the system user.");
    }

    private boolean ensurePilot() {
        if (currentSession == null) {
            return false;
        }
        return currentSession.authenticatedUser().hasAny(Roles.PILOT);
    }

    private String authError() {
        if (currentSession == null) {
            return "ERROR|NOT_AUTHENTICATED";
        }
        return "ERROR|FORBIDDEN";
    }

    private AuthorizationPort authorizationPort() {
        final SystemUser user = currentSession.authenticatedUser();
        return new AuthorizationPort() {
            @Override
            public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
                if (!user.hasAny(roles)) {
                    throw new SecurityException("User not authorized");
                }
            }

            @Override
            public String currentUsername() {
                if (user.email() != null && !user.email().toString().isBlank()) {
                    return user.email().toString().strip();
                }
                return user.username().toString();
            }
        };
    }

    private static Instant parseInstant(final String raw) {
        final String value = raw == null ? "" : raw.trim();
        if (value.endsWith("Z") || value.contains("+")) {
            return Instant.parse(value);
        }
        return Instant.parse(value + "Z");
    }

    private static String sanitize(final String message) {
        if (message == null || message.isBlank()) {
            return "UNKNOWN";
        }
        return message.replace('|', ' ').replace('\n', ' ').trim();
    }

    private static String sanitizeMultiline(final String message) {
        if (message == null || message.isBlank()) {
            return "UNKNOWN";
        }
        return message.replace('|', ' ').trim();
    }
}
