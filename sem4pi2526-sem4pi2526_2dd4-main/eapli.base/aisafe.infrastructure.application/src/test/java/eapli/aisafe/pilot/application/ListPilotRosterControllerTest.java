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
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.FakeAuth;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestAircraftModelRepo;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.pilot.application.PilotRosterListingTestDoubles.TestPilotRepo;
import eapli.aisafe.pilot.application.dto.PilotRosterListResult;
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
 * List pilot roster for the authenticated ATCC's company.
 * Focus: orchestration, authorization and mapping (domain eligibility tested separately).
 */
@DisplayName("ListPilotRosterController")
class ListPilotRosterControllerTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);

    private FakeAuth auth;
    private TestPilotRepo pilotRepo;
    private TestCompanyRepo companyRepo;
    private TestAircraftModelRepo modelRepo;
    private ListPilotRosterController controller;
    private CompanyId companyId;
    private List<AircraftModelId> certifiedModels;

    @BeforeAll
    static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @BeforeEach
    void setUp() {
        auth = new FakeAuth();
        pilotRepo = new TestPilotRepo();
        companyRepo = new TestCompanyRepo();
        modelRepo = new TestAircraftModelRepo();
        companyId = new CompanyId(UUID.randomUUID());
        companyRepo.linkedCompany = Optional.of(companyId);
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN, 2, 180, 6000.0,
                List.of(EngineModelId.newId()));
        modelRepo.store.put(model.identity(), model);
        certifiedModels = List.of(model.identity());
        controller = new ListPilotRosterController(auth, pilotRepo, companyRepo, modelRepo);
    }

    private Pilot pilotNamed(final String first, final String last, final String email) {
        return new Pilot(
                PilotId.newId(),
                new FullName(first, last),
                new Email(email),
                companyId,
                new PilotCertification("ATPL"),
                new License("LIC-" + last, FUTURE),
                certifiedModels);
    }

    @Test
    @DisplayName("default constructor wires dependencies via RepositoryFactory")
    void defaultConstructorShouldWireDependencies() {
        // Act
        final ListPilotRosterController ctrl = new ListPilotRosterController();

        // Assert
        assertNotNull(ctrl);
    }

    @Test
    @DisplayName("success: returns active pilots as DTOs sorted by full name")
    void listShouldReturnActivePilotsSortedByFullName() {
        // Arrange
        final Pilot rui = pilotNamed("Rui", "Costa", "rui@tap.pt");
        final Pilot ana = pilotNamed("Ana", "Silva", "ana@tap.pt");
        pilotRepo.save(rui);
        pilotRepo.save(ana);

        // Act
        final PilotRosterListResult result = controller.listPilotRoster();

        // Assert
        assertArrayEquals(new Role[] { Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR }, auth.lastRoles);
        assertEquals(companyId, pilotRepo.lastQueriedCompanyId);
        assertEquals(2, result.pilots().size());
        assertNull(result.messageWhenEmpty());
        assertEquals("Ana Silva", result.pilots().get(0).fullName());
        assertEquals("Rui Costa", result.pilots().get(1).fullName());
        assertEquals("ana@tap.pt", result.pilots().get(0).email());
        assertEquals("ATPL", result.pilots().get(0).certification());
        assertEquals("LIC-Silva", result.pilots().get(0).licenseNumber());
        assertEquals(FUTURE.toString(), result.pilots().get(0).licenseExpiration());
        assertEquals("ACTIVE", result.pilots().get(0).status());
        assertEquals(1, result.pilots().get(0).certifiedModels().size());
        assertEquals("A320 / Airbus", result.pilots().get(0).certifiedModels().get(0));
        assertEquals(rui.identity().toString(), result.pilots().get(1).pilotId());
    }

    @Test
    @DisplayName("empty list: returns message when company has no active pilots")
    void listShouldReturnEmptyResultWithMessage() {
        // Arrange — no pilots in repository response

        // Act
        final PilotRosterListResult result = controller.listPilotRoster();

        // Assert
        assertTrue(result.pilots().isEmpty());
        assertEquals(PilotRosterListResult.NO_ACTIVE_PILOTS, result.messageWhenEmpty());
    }

    @Test
    @DisplayName("orchestration: repository query is scoped to authenticated company")
    void listShouldQueryPilotsByAuthenticatedCompany() {
        // Arrange
        pilotRepo.save(pilotNamed("Ana", "Silva", "ana@tap.pt"));

        // Act
        controller.listPilotRoster();

        // Assert
        assertEquals(companyId, pilotRepo.lastQueriedCompanyId);
    }

    @Test
    @DisplayName("orchestration: inactive pilots are excluded by findActiveByCompanyId contract")
    void listShouldRelyOnRepositoryToExcludeInactivePilots() {
        // Arrange
        final Pilot active = pilotNamed("Ana", "Silva", "ana@tap.pt");
        final Pilot inactive = pilotNamed("Rui", "Costa", "rui@tap.pt");
        inactive.setStatus(PilotStatus.INACTIVE);
        pilotRepo.save(active);
        pilotRepo.save(inactive);

        // Act
        final PilotRosterListResult result = controller.listPilotRoster();

        // Assert
        assertEquals(1, result.pilots().size());
        assertEquals("Ana Silva", result.pilots().get(0).fullName());
        assertEquals("ACTIVE", result.pilots().get(0).status());
    }

    @Test
    @DisplayName("failure: rejects when user lacks AIR_TRANSPORT_COMPANY_COLLABORATOR role")
    void listShouldFailWhenUserLacksRequiredRole() {
        // Arrange
        auth.denyAuthorization = true;
        pilotRepo.save(pilotNamed("Ana", "Silva", "ana@tap.pt"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> controller.listPilotRoster());
        assertNull(pilotRepo.lastQueriedCompanyId);
    }

    @Test
    @DisplayName("mapping: uses model id when aircraft model is not found in repository")
    void listShouldFallbackToModelIdWhenAircraftModelNotFound() {
        // Arrange
        final AircraftModelId unknownModelId = AircraftModelId.newId();
        final Pilot pilot = new Pilot(
                PilotId.newId(),
                new FullName("Ana", "Silva"),
                new Email("ana@tap.pt"),
                companyId,
                new PilotCertification("ATPL"),
                new License("LIC-Silva", FUTURE),
                List.of(unknownModelId));
        pilotRepo.save(pilot);

        // Act
        final PilotRosterListResult result = controller.listPilotRoster();

        // Assert
        assertEquals(1, result.pilots().size());
        assertEquals(unknownModelId.toString(), result.pilots().get(0).certifiedModels().get(0));
    }

    @Test
    @DisplayName("failure: rejects when collaborator is not linked to any company")
    void listShouldFailWhenUserNotLinkedToCompany() {
        // Arrange
        companyRepo.linkedCompany = Optional.empty();

        // Act & Assert
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.listPilotRoster());
        assertTrue(ex.getMessage().contains("not linked"));
    }

    @Test
    @DisplayName("orchestration: list operation does not mutate persisted pilots")
    void listShouldNotMutatePersistedPilots() {
        // Arrange
        pilotRepo.save(pilotNamed("Ana", "Silva", "ana@tap.pt"));
        final long pilotsBefore = pilotRepo.count();

        // Act
        controller.listPilotRoster();

        // Assert
        assertEquals(pilotsBefore, pilotRepo.count());
    }
}
