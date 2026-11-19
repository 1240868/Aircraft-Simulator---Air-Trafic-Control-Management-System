package eapli.aisafe.pilot.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.pilot.License;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotCertification;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.FakeAuth;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestFlightPlanRepo;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestPilotRepo;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.framework.infrastructure.authz.domain.model.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deactivate a pilot from the authenticated ATCC's company roster.
 * Focus: authorization, company scope, orchestration and persistence.
 */
@DisplayName("DeactivatePilotController")
class DeactivatePilotControllerTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);
    private static final List<AircraftModelId> MODELS = List.of(AircraftModelId.newId());

    private FakeAuth auth;
    private TestPilotRepo pilotRepo;
    private TestCompanyRepo companyRepo;
    private TestFlightPlanRepo flightPlanRepo;
    private DeactivatePilotController controller;
    private CompanyId companyId;

    @BeforeAll
    static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @BeforeEach
    void setUp() {
        auth = new FakeAuth();
        pilotRepo = new TestPilotRepo();
        companyRepo = new TestCompanyRepo();
        flightPlanRepo = new TestFlightPlanRepo();
        companyId = new CompanyId(UUID.randomUUID());
        companyRepo.linkedCompany = Optional.of(companyId);
        controller = new DeactivatePilotController(auth, pilotRepo, companyRepo, flightPlanRepo);
    }

    private Pilot pilotInCompany(final CompanyId company, final String first, final String last, final String email) {
        return new Pilot(
                PilotId.newId(),
                new FullName(first, last),
                new Email(email),
                company,
                new PilotCertification("ATPL"),
                new License("LIC-" + last, FUTURE),
                MODELS);
    }

    @Test
    @DisplayName("default constructor wires dependencies via RepositoryFactory")
    void defaultConstructorShouldWireDependencies() {
        // Act
        final DeactivatePilotController ctrl = new DeactivatePilotController();

        // Assert
        assertNotNull(ctrl);
    }

    @Test
    @DisplayName("success: deactivates an ACTIVE pilot from the authenticated company")
    void deactivateShouldSetPilotInactiveAndPersist() {
        // Arrange
        final Pilot ana = pilotInCompany(companyId, "Ana", "Silva", "ana@tap.pt");
        pilotRepo.save(ana);

        // Act
        controller.deactivatePilot(ana.identity());

        // Assert
        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(ana.identity(), flightPlanRepo.lastCheckedPilotId);
        assertEquals(1, pilotRepo.saveCount);
        assertEquals(PilotStatus.INACTIVE, pilotRepo.lastSavedPilot.getStatus());
        assertEquals(ana.identity(), pilotRepo.lastSavedPilot.identity());
    }

    @Test
    @DisplayName("failure: rejects when user lacks AIR_TRANSPORT_COMPANY_COLLABORATOR role")
    void deactivateShouldFailWhenUserLacksRequiredRole() {
        // Arrange
        auth.denyAuthorization = true;
        final Pilot ana = pilotInCompany(companyId, "Ana", "Silva", "ana@tap.pt");
        pilotRepo.save(ana);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> controller.deactivatePilot(ana.identity()));
        assertEquals(0, pilotRepo.saveCount);
        assertNull(flightPlanRepo.lastCheckedPilotId);
    }

    @Test
    @DisplayName("failure: rejects when pilot is not ACTIVE")
    void deactivateShouldFailWhenPilotIsNotActive() {
        // Arrange
        final Pilot ana = pilotInCompany(companyId, "Ana", "Silva", "ana@tap.pt");
        ana.setStatus(PilotStatus.INACTIVE);
        pilotRepo.save(ana);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> controller.deactivatePilot(ana.identity()));
        assertEquals(0, pilotRepo.saveCount);
    }

    @Test
    @DisplayName("failure: rejects when pilot has assigned flight plans")
    void deactivateShouldFailWhenPilotHasAssignedFlightPlans() {
        // Arrange
        final Pilot ana = pilotInCompany(companyId, "Ana", "Silva", "ana@tap.pt");
        pilotRepo.save(ana);
        flightPlanRepo.pilotsWithAssignedPlans.add(ana.identity());

        // Act & Assert
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.deactivatePilot(ana.identity()));
        assertTrue(ex.getMessage().toLowerCase().contains("flight plan"));
        assertEquals(0, pilotRepo.saveCount);
        assertEquals(PilotStatus.ACTIVE, pilotRepo.ofIdentity(ana.identity()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("failure: rejects pilot belonging to another company")
    void deactivateShouldFailWhenPilotBelongsToAnotherCompany() {
        // Arrange
        final CompanyId otherCompany = new CompanyId(UUID.randomUUID());
        final Pilot outsider = pilotInCompany(otherCompany, "Rui", "Costa", "rui@tap.pt");
        pilotRepo.save(outsider);

        // Act & Assert
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.deactivatePilot(outsider.identity()));
        assertTrue(ex.getMessage().toLowerCase().contains("company"));
        assertEquals(0, pilotRepo.saveCount);
    }

    @Test
    @DisplayName("failure: rejects when pilot is not found")
    void deactivateShouldFailWhenPilotNotFound() {
        // Arrange
        final PilotId unknown = PilotId.newId();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.deactivatePilot(unknown));
        assertEquals(0, pilotRepo.saveCount);
    }

    @Test
    @DisplayName("failure: rejects when collaborator is not linked to any company")
    void deactivateShouldFailWhenUserNotLinkedToCompany() {
        // Arrange
        companyRepo.linkedCompany = Optional.empty();
        final Pilot ana = pilotInCompany(companyId, "Ana", "Silva", "ana@tap.pt");
        pilotRepo.save(ana);

        // Act & Assert
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.deactivatePilot(ana.identity()));
        assertTrue(ex.getMessage().contains("not linked"));
        assertEquals(0, pilotRepo.saveCount);
    }
}
