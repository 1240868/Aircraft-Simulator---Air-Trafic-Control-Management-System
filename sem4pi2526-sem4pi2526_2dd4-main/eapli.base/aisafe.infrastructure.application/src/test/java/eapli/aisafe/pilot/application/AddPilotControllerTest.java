package eapli.aisafe.pilot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@DisplayName("AddPilotController")
class AddPilotControllerTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);

    private static final class FakeAuth implements AuthorizationPort {
        Role[] last;
        String username = "atcc1";

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.last = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    private static final class TestPilotRepo implements PilotRepository {

        private final Map<PilotId, Pilot> store = new HashMap<>();

        @Override
        public Pilot save(final Pilot entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Pilot> ofIdentity(final PilotId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Pilot> ofIdentity(final String id) {
            final PilotId pid = new PilotId(UUID.fromString(id));
            for (final Pilot p : store.values()) {
                if (p.identity().equals(pid)) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }

        @Override
        public Iterable<Pilot> findAll() {
            return store.values();
        }

        @Override
        public Optional<Pilot> findByEmail(final Email email) {
            for (final Pilot p : store.values()) {
                if (p.getEmail().value().equals(email.value())) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }

        @Override
        public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
            return store.values().stream()
                    .filter(p -> p.getCompany().equals(companyId) && p.isEligibleForRosterListing())
                    .toList();
        }

        @Override
        public void delete(final Pilot entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final PilotId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private static final class TestCompanyRepo implements AirTransportCompanyRepository {

        private final Map<CompanyId, AirTransportCompany> store = new HashMap<>();

        @Override
        public AirTransportCompany save(final AirTransportCompany entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirTransportCompany> ofIdentity(final CompanyId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AirTransportCompany> findAll() {
            return store.values();
        }

        @Override
        public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            return Optional.empty();
        }

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            for (final AirTransportCompany c : store.values()) {
                if (c.collaboratorUsernames().contains(systemUsername.strip())) {
                    return Optional.of(c.identity());
                }
            }
            return Optional.empty();
        }

        @Override
        public void delete(final AirTransportCompany entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final CompanyId id) {
            store.remove(id);
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            for (final var c : store.values()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private static final class TestAircraftModelRepo implements AircraftModelRepository {

        private final Map<AircraftModelId, AircraftModel> store = new HashMap<>();

        @Override
        public AircraftModel save(final AircraftModel entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AircraftModel> ofIdentity(final AircraftModelId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AircraftModel> findAll() {
            return store.values();
        }

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                       final String manufacturer) {
            return Optional.empty();
        }

        @Override
        public void delete(final AircraftModel entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final AircraftModelId id) {
            store.remove(id);
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            return ofIdentity(new AircraftModelId(UUID.fromString(id)));
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            return ofIdentity(id);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private TestPilotRepo pilotRepo;
    private TestCompanyRepo companyRepo;
    private TestAircraftModelRepo modelRepo;
    private FakeAuth auth;
    private UserManagementService userSvc;
    private AddPilotController controller;
    private CompanyId companyId;
    private AircraftModelId modelId;

    @BeforeAll
    static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @BeforeEach
    void setUp() {
        pilotRepo = new TestPilotRepo();
        companyRepo = new TestCompanyRepo();
        modelRepo = new TestAircraftModelRepo();
        auth = new FakeAuth();
        userSvc = Mockito.mock(UserManagementService.class);
        auth.username = "atcc-" + UUID.randomUUID().toString().substring(0, 8);
        controller = new AddPilotController(auth, userSvc, pilotRepo, companyRepo, modelRepo);

        final var company = new AirTransportCompany(
                new CompanyName("TAP Air Portugal"),
                new CompanyIataCode("TP"),
                new CompanyIcaoCode("TAP"),
                new AirOperatorCertificate("AOC-TAP-1", LocalDate.now().plusYears(1)));
        company.addCollaborator(auth.username);
        companyRepo.save(company);
        companyId = company.identity();

        final AircraftModel model = new AircraftModel(
                "A320neo", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000.0, List.of(EngineModelId.newId()));
        modelRepo.save(model);
        modelId = model.identity();
    }

    @Test
    @DisplayName("happy path: persists pilot for collaborator company")
    void addPilotPersists() {
        final Pilot pilot = controller.addPilot(
                "ana.silva", "Password1", "Ana", "Silva", "ana.silva@tap.pt",
                "ATPL", "PT-LIC-001", FUTURE, List.of(modelId));

        assertNotNull(pilot);
        assertEquals(PilotStatus.ACTIVE, pilot.getStatus());
        assertEquals(companyId, pilot.getCompany());
        assertEquals("ana.silva@tap.pt", pilot.getEmail().value());
        assertEquals(1, countPilots());
    }

    @Test
    @DisplayName("pilot name, license and certified models are stored correctly")
    void addPilotStoresAllFields() {
        final Pilot pilot = controller.addPilot(
                "rui.costa", "Password1", "Rui", "Costa", "rui.costa@tap.pt",
                "CPL", "PT-LIC-002", FUTURE, List.of(modelId));

        assertEquals("Rui", pilot.getName().firstName());
        assertEquals("Costa", pilot.getName().lastName());
        assertEquals("PT-LIC-002", pilot.getLicenseVoo().value());
        assertEquals(FUTURE, pilot.getLicenseVoo().expirationDate());
        assertEquals("CPL", pilot.getCertification().value());
        assertEquals(1, pilot.getCertifiedModels().size());
        assertEquals(modelId, pilot.getCertifiedModels().get(0));
    }

    @Test
    @DisplayName("rejects duplicate email")
    void addPilotFailsOnDuplicateEmail() {
        controller.addPilot("user1", "Pass1", "Ana", "Silva", "dup@tap.pt", "ATPL", "LIC-1", FUTURE, List.of(modelId));

        final IntegrityViolationException ex = assertThrows(IntegrityViolationException.class,
                () -> controller.addPilot("user2", "Pass1", "Dup", "Pilot", "dup@tap.pt", "CPL", "LIC-2", FUTURE, List.of(modelId)));
        assertTrue(ex.getMessage().contains("dup@tap.pt"));
        assertEquals(1, countPilots());
    }

    @Test
    @DisplayName("two distinct emails produce two distinct pilots")
    void addPilotAllowsDifferentEmails() {
        controller.addPilot("user1", "Pass1", "Ana", "Silva", "ana@tap.pt", "ATPL", "LIC-1", FUTURE, List.of(modelId));
        controller.addPilot("user2", "Pass1", "Rui", "Costa", "rui@tap.pt", "CPL", "LIC-2", FUTURE, List.of(modelId));
        assertEquals(2, countPilots());
    }

    @Test
    @DisplayName("resolves company of authenticated collaborator")
    void resolveCompanyReturnsLinkedCompany() {
        assertEquals(companyId, controller.resolveCompanyOfAuthenticatedUser());
    }

    @Test
    @DisplayName("fails when collaborator is not linked to any company")
    void resolveCompanyFailsWhenUserNotLinked() {
        auth.username = "nobody";

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.resolveCompanyOfAuthenticatedUser());
        assertTrue(ex.getMessage().contains("not linked"));
    }

    @Test
    @DisplayName("rejects empty certified models list")
    void addPilotFailsOnEmptyCertifiedModels() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "x@tap.pt", "ATPL", "LIC-X", FUTURE, List.of()));
    }

    @Test
    @DisplayName("rejects unknown aircraft model id")
    void addPilotFailsOnUnknownModelId() {
        final AircraftModelId unknown = AircraftModelId.newId();
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "x@tap.pt", "ATPL", "LIC-X", FUTURE, List.of(unknown)));
    }

    @Test
    @DisplayName("rejects empty first name")
    void addPilotFailsOnEmptyFirstName() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "", "Silva", "x@tap.pt", "ATPL", "LIC-X", FUTURE, List.of(modelId)));
    }

    @Test
    @DisplayName("rejects empty last name")
    void addPilotFailsOnEmptyLastName() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "", "x@tap.pt", "ATPL", "LIC-X", FUTURE, List.of(modelId)));
    }

    @Test
    @DisplayName("rejects invalid email format")
    void addPilotFailsOnInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "not-an-email", "ATPL", "LIC-X", FUTURE, List.of(modelId)));
    }

    @Test
    @DisplayName("rejects empty certification")
    void addPilotFailsOnEmptyCertification() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "x@tap.pt", "", "LIC-X", FUTURE, List.of(modelId)));
    }

    @Test
    @DisplayName("rejects empty license number")
    void addPilotFailsOnEmptyLicenseNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "x@tap.pt", "ATPL", "", FUTURE, List.of(modelId)));
    }

    @Test
    @DisplayName("rejects past license expiration")
    void addPilotFailsOnPastLicenseExpiration() {
        final LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> controller.addPilot("user1", "Pass1", "Ana", "Silva", "x@tap.pt", "ATPL", "LIC-X", past, List.of(modelId)));
    }

    private int countPilots() {
        int n = 0;
        for (final Pilot ignored : pilotRepo.findAll()) {
            n++;
        }
        return n;
    }
}
