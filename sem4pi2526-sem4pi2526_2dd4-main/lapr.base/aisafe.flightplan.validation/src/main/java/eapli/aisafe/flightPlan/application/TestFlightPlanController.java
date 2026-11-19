package eapli.aisafe.flightPlan.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** US085 — test/validate a pilot's flight plan (DSL + C physics). */
@UseCaseController
public class TestFlightPlanController {

    private static final Path DEFAULT_VALIDATOR_DIR = Paths.get("lapr.base");

    private final AuthorizationPort authz;
    private final FlightPlanRepository flightPlanRepo;
    private final PilotRepository pilotRepo;
    private final FlightPlanDslValidationPort dslValidator;
    private final FlightPlanTestPayloadWriter payloadWriter;
    private final FlightPlanPhysicsTestPort physicsTester;

    public TestFlightPlanController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().flightPlans(),
                PersistenceContext.repositories().pilots(),
                new FlightPlanDslFileValidator(),
                new FlightPlanTestPayloadBuilder(
                        PersistenceContext.repositories().aircraft(),
                        PersistenceContext.repositories().aircraftModels(),
                        PersistenceContext.repositories().airports())::writePayload,
                new FlightPlanPhysicsValidator(DEFAULT_VALIDATOR_DIR));
    }

    public TestFlightPlanController(final AuthorizationPort authz,
                                    final FlightPlanRepository flightPlanRepo,
                                    final PilotRepository pilotRepo,
                                    final FlightPlanDslValidationPort dslValidator,
                                    final FlightPlanTestPayloadWriter payloadWriter,
                                    final FlightPlanPhysicsTestPort physicsTester) {
        this.authz = authz;
        this.flightPlanRepo = flightPlanRepo;
        this.pilotRepo = pilotRepo;
        this.dslValidator = dslValidator;
        this.payloadWriter = payloadWriter;
        this.physicsTester = physicsTester;
    }

    public List<FlightPlan> listTestablePlansForCurrentPilot() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);
        final PilotId pilotId = resolvePilotIdForCurrentUser();
        final List<FlightPlan> plans = new ArrayList<>();
        for (final FlightPlan plan : flightPlanRepo.findAll()) {
            if (pilotId.equals(plan.getPilot()) && isTestable(plan.getStatus())) {
                plans.add(plan);
            }
        }
        plans.sort(Comparator.comparing(fp -> fp.getDesignator().toString()));
        return plans;
    }

    public FlightPlanTestResult testForCurrentPilot(final FlightPlanId flightPlanId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);
        return test(flightPlanId, resolvePilotIdForCurrentUser());
    }

    public FlightPlanTestResult test(final FlightPlanId flightPlanId, final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.PILOT);

        final FlightPlan plan = flightPlanRepo.findById(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan not found: " + flightPlanId));

        if (plan.getPilot() == null || !pilotId.equals(plan.getPilot())) {
            throw new IllegalArgumentException("Flight plan does not belong to the authenticated pilot");
        }

        if (!isTestable(plan.getStatus())) {
            throw new IllegalStateException("Flight plan in status " + plan.getStatus() + " cannot be tested");
        }

        final List<String> dslErrors = dslValidator.validate(plan);
        if (!dslErrors.isEmpty()) {
            plan.markTestFailed();
            flightPlanRepo.save(plan);
            return FlightPlanTestResult.failed(dslErrors, List.of(), FlightPlanStatus.REJECTED);
        }

        Path payload = null;
        try {
            payload = payloadWriter.writePayload(plan);
            final List<String> physicsErrors = physicsTester.run(payload);
            if (!physicsErrors.isEmpty()) {
                plan.markTestFailed();
                flightPlanRepo.save(plan);
                return FlightPlanTestResult.failed(List.of(), physicsErrors, FlightPlanStatus.REJECTED);
            }

            plan.markTestPassed();
            flightPlanRepo.save(plan);
            return FlightPlanTestResult.success();

        } catch (final IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to build test payload: " + ex.getMessage(), ex);
        } finally {
            if (payload != null) {
                try {
                    Files.deleteIfExists(payload);
                } catch (final IOException ignored) {
                }
            }
        }
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

    static boolean isTestable(final FlightPlanStatus status) {
        return status == FlightPlanStatus.DRAFT
                || status == FlightPlanStatus.PENDING_TEST
                || status == FlightPlanStatus.REJECTED;
    }
}
