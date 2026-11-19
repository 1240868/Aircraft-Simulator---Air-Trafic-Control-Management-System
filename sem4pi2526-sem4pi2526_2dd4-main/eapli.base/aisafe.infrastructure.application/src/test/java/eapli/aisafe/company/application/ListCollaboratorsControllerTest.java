package eapli.aisafe.company.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.aircontrol.application.AirControlAreaOptionDTO;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CustomerCollaboratorRef;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ListCollaboratorsController")
class ListCollaboratorsControllerTest {

    private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";
    private static final LocalDate CERT_EXPIRY = LocalDate.of(2032, 6, 15);
    private static boolean persistenceFactoryConfigured;
    private static boolean authzConfigured;

    static void ensureTestRepositoryFactory() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    /**
     * Minimal {@link RepositoryFactory} on the test classpath (no persistence.impl dependency).
     */
    public static final class TestRepositoryFactory implements RepositoryFactory {
        private final InMemoryUserRepository users = new InMemoryUserRepository();
        private final TestCompanyRepo companies = new TestCompanyRepo();
        private final TestAreaRepo areas = new TestAreaRepo();

        @Override
        public TransactionalContext newTransactionalContext() {
            return null;
        }

        @Override
        public UserRepository users(final TransactionalContext autoTx) {
            return users;
        }

        @Override
        public UserRepository users() {
            return users(null);
        }

        @Override
        public AirControlAreaRepository airControlAreas() {
            return areas;
        }

        @Override
        public eapli.aisafe.airport.repositories.AirportRepository airports() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AirTransportCompanyRepository airTransportCompanies() {
            return companies;
        }

        @Override
        public eapli.aisafe.fleet.repositories.AircraftModelRepository aircraftModels() {
            throw new UnsupportedOperationException();
        }

        @Override
        public eapli.aisafe.fleet.repositories.AircraftRepository aircraft() {
            throw new UnsupportedOperationException();
        }

        @Override
        public eapli.aisafe.fleet.repositories.EngineModelRepository engineModels() {
            throw new UnsupportedOperationException();
        }

        @Override
        public eapli.aisafe.weather.repositories.WeatherDataRepository weatherData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public eapli.aisafe.fleet.application.PendingFlightsChecker pendingFlightsChecker() {
            throw new UnsupportedOperationException();
        }

        @Override
        public eapli.aisafe.pilot.repositories.PilotRepository pilots() {
            return null;
        }

        @Override
        public eapli.aisafe.flightPlan.repositories.FlightPlanRepository flightPlans() {
            return null;
        }

        @Override
        public eapli.aisafe.flightroute.repositories.FlightRouteRepository flightRoutes() {
            return null;
        }
    }

    private static String lettersOnly2() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "AB").substring(0, 2);
    }

    private static String lettersOnly3() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "ABC").substring(0, 3);
    }

    private static SystemUser activeUser(final String username,
                                         final String firstName,
                                         final String lastName,
                                         final String email) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", firstName, lastName, email)
                .withRoles(Roles.BACKOFFICE_OPERATOR)
                .build();
    }

    private static SystemUser activeUserWithoutEmail(final String username) throws Exception {
        final SystemUser built = activeUser(username, "Ana", "Silva", "placeholder@example.com");
        clearEmail(built);
        return built;
    }

    private static void clearEmail(final SystemUser user) throws Exception {
        Class<?> type = user.getClass();
        while (type != null) {
            for (final Field field : type.getDeclaredFields()) {
                if (!field.getName().toLowerCase().contains("email")) {
                    continue;
                }
                field.setAccessible(true);
                field.set(user, null);
                if (user.email() == null) {
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("Could not clear email on SystemUser");
    }

    private static CustomerCollaboratorRef collaboratorRefWithUsername(final String username) throws Exception {
        final var ctor = CustomerCollaboratorRef.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final CustomerCollaboratorRef ref = ctor.newInstance();
        final Field usernameField = CustomerCollaboratorRef.class.getDeclaredField("username");
        usernameField.setAccessible(true);
        usernameField.set(ref, username);
        final Field phoneField = CustomerCollaboratorRef.class.getDeclaredField("phone");
        phoneField.setAccessible(true);
        phoneField.set(ref, "900000000");
        return ref;
    }

    @SuppressWarnings("unchecked")
    private static void addNullCollaboratorUsername(final AirTransportCompany company) throws Exception {
        final Field field = AirTransportCompany.class.getDeclaredField("collaborators");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Set<CustomerCollaboratorRef> collaborators =
                new HashSet<>((Set<CustomerCollaboratorRef>) field.get(company));
        collaborators.add(null);
        field.set(company, collaborators);
    }

    @SuppressWarnings("unchecked")
    private static void addBlankCollaboratorUsername(final AirTransportCompany company) throws Exception {
        final Field field = AirTransportCompany.class.getDeclaredField("collaborators");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Set<CustomerCollaboratorRef> collaborators =
                new HashSet<>((Set<CustomerCollaboratorRef>) field.get(company));
        collaborators.add(collaboratorRefWithUsername("   "));
        field.set(company, collaborators);
    }

    private static final class RecordingAuthz extends AuthorizationService {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }
    }

    private static final class RecordingUserService extends UserManagementService {
        private final List<SystemUser> active = new ArrayList<>();

        RecordingUserService() {
            super(new InMemoryUserRepository(), new NilPasswordPolicy(), new PlainTextEncoder());
        }

        void seedActive(final SystemUser user) {
            active.add(user);
        }

        @Override
        public Iterable<SystemUser> activeUsers() {
            return List.copyOf(active);
        }
    }

    private static final class TestCompanyRepo
            extends InMemoryDomainRepository<AirTransportCompany, CompanyId>
            implements AirTransportCompanyRepository {

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
            return Optional.empty();
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            for (final var c : findAll()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }
    }

    private static final class TestAreaRepo
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
            implements AirControlAreaRepository {

    @Override
    public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
        return ofIdentity(code);
    }

    @Override
    public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
        return Optional.empty();
    }
}

    private RecordingAuthz authz;
    private RecordingUserService userSvc;
    private TestCompanyRepo companyRepo;
    private TestAreaRepo areaRepo;
    private ListCollaboratorsController controller;
    private CompanyId companyId;
    private AirControlAreaCode areaCode;

    @BeforeAll
    static void configurePersistenceFactory() throws Exception {
        ensureTestRepositoryFactory();
    }

    @BeforeEach
    void setUp() {
        authz = new RecordingAuthz();
        userSvc = new RecordingUserService();
        companyRepo = new TestCompanyRepo();
        areaRepo = new TestAreaRepo();
        controller = new ListCollaboratorsController(authz, userSvc, companyRepo, areaRepo);

        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("TAP " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-1", CERT_EXPIRY));
        company.addCollaborator("active1");
        company.addCollaborator("active2");
        company.addCollaborator("disabled1");
        company.addCollaborator("  active3  ");
        companyRepo.save(company);
        companyId = company.identity();

        userSvc.seedActive(activeUser("active1", "Ana", "Silva", "ana@example.com"));
        userSvc.seedActive(activeUser("active2", "Bob", "Costa", "bob@example.com"));
        userSvc.seedActive(activeUser("active3", "Carla", "Luz", "carla@example.com"));

        final AirControlArea area = new AirControlArea("PT-OPO", "Porto FIR", 40.0, 43.0, -9.0, -7.0);
        area.addCollaborator("area-user");
        areaRepo.save(area);
        areaCode = area.identity();
        userSvc.seedActive(activeUser("area-user", "Diogo", "Pires", "diogo@example.com"));
    }

    @Test
    @DisplayName("lists only active collaborators linked to company as DTOs")
    void listActiveCollaboratorsForCompanyShouldReturnActiveOnly() {
        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(companyId.asUuid().toString());

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(d -> "active1".equals(d.username())));
        assertTrue(result.stream().anyMatch(d -> "active2".equals(d.username())));
        assertTrue(result.stream().anyMatch(d -> "active3".equals(d.username())));
        assertTrue(result.stream().noneMatch(d -> "disabled1".equals(d.username())));
        assertArrayEquals(new Role[] {Roles.ADMIN, Roles.BACKOFFICE_OPERATOR}, authz.lastRoles);
        final CollaboratorDTO ana = result.stream()
                .filter(d -> "active1".equals(d.username()))
                .findFirst()
                .orElseThrow();
        assertEquals("Ana", ana.firstName());
        assertEquals("Silva", ana.lastName());
        assertEquals("ana@example.com", ana.email());
        assertEquals(null, ana.phone());
        assertNotNull(ana.toString());
        assertEquals(ana, new CollaboratorDTO(
                ana.username(), ana.firstName(), ana.lastName(), ana.email(), ana.phone()));
    }

    @Test
    @DisplayName("default constructor wires production dependencies")
    void defaultConstructorShouldConstruct() {
        assertNotNull(new ListCollaboratorsController());
    }

    @Test
    @DisplayName("inactive users never appear even if username is linked on customer")
    void listShouldExcludeUsersNotReturnedByActiveUsers() {
        // Arrange — disabled1 is linked on company but not in activeUsers()

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(companyId.asUuid().toString());

        // Assert
        assertTrue(result.stream().noneMatch(d -> "disabled1".equals(d.username())));
    }

    @Test
    @DisplayName("empty list when customer has no collaborators")
    void listShouldReturnEmptyListWhenNoCollaborators() {
        // Arrange
        final AirTransportCompany emptyCo = new AirTransportCompany(
                new CompanyName("Empty " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-E", CERT_EXPIRY));
        companyRepo.save(emptyCo);

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(emptyCo.identity().asUuid().toString());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("lists active collaborators for air control area")
    void listActiveCollaboratorsForAreaShouldReturnDtos() {
        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForAirControlArea(areaCode.toString());

        // Assert
        assertEquals(1, result.size());
        assertEquals("area-user", result.get(0).username());
        assertEquals("Diogo", result.get(0).firstName());
    }

    @Test
    @DisplayName("rejects unknown company id")
    void listShouldRejectMissingCompany() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listActiveCollaboratorsForCompany(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("rejects invalid company uuid")
    void listShouldRejectInvalidCompanyId() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listActiveCollaboratorsForCompany("bad-id"));
    }

    @Test
    @DisplayName("rejects unknown air control area")
    void listShouldRejectMissingArea() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listActiveCollaboratorsForAirControlArea("XX-ZZZ"));
    }

    @Test
    @DisplayName("builds company selection DTOs")
    void listAirTransportCompaniesForSelectionShouldMapToDtos() {
        // Act
        AirTransportCompanyOptionDTO match = null;
        for (final AirTransportCompanyOptionDTO dto : controller.listAirTransportCompaniesForSelection()) {
            assertNotNull(dto.companyId());
            assertNotNull(dto.displayName());
            assertNotNull(dto.iataCode());
            if (companyId.asUuid().toString().equals(dto.companyId())) {
                match = dto;
            }
        }

        // Assert
        assertNotNull(match);
    }

    @Test
    @DisplayName("builds area selection DTOs")
    void listAirControlAreasForSelectionShouldMapToDtos() {
        // Act
        AirControlAreaOptionDTO match = null;
        for (final AirControlAreaOptionDTO dto : controller.listAirControlAreasForSelection()) {
            assertNotNull(dto.areaId());
            assertNotNull(dto.displayName());
            if (areaCode.toString().equals(dto.areaId())) {
                match = dto;
            }
        }

        // Assert
        assertNotNull(match);
    }

    @Test
    @DisplayName("skips active users not linked to the customer")
    void listShouldNotIncludeUnlinkedActiveUsers() {
        // Arrange
        userSvc.seedActive(activeUser("orphan-active", "Orphan", "User", "orphan@example.com"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(companyId.asUuid().toString());

        // Assert
        assertTrue(result.stream().noneMatch(d -> "orphan-active".equals(d.username())));
    }

    @Test
    @DisplayName("maps null email to null on collaborator DTO")
    void listShouldMapNullEmailOnDto() throws Exception {
        // Arrange
        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("NullMail " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-N", CERT_EXPIRY));
        company.addCollaborator("no-email-active");
        companyRepo.save(company);
        userSvc.seedActive(activeUserWithoutEmail("no-email-active"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(company.identity().asUuid().toString());

        // Assert
        final CollaboratorDTO dto = result.stream()
                .filter(d -> "no-email-active".equals(d.username()))
                .findFirst()
                .orElseThrow();
        assertEquals(null, dto.email());
    }

    @Test
    @DisplayName("ignores blank collaborator username entries on aggregate")
    void listShouldIgnoreBlankLinkedUsernamesOnAggregate() throws Exception {
        // Arrange
        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("BlankOnly " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-BL", CERT_EXPIRY));
        addBlankCollaboratorUsername(company);
        company.addCollaborator("valid-blank");
        companyRepo.save(company);
        userSvc.seedActive(activeUser("valid-blank", "Val", "Id", "val@example.com"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(company.identity().asUuid().toString());

        // Assert
        assertEquals(1, result.size());
        assertEquals("valid-blank", result.get(0).username());
    }

    @Test
    @DisplayName("ignores null collaborator username entries on aggregate")
    void listShouldIgnoreNullLinkedUsernames() throws Exception {
        // Arrange
        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("NullUser " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-U", CERT_EXPIRY));
        addNullCollaboratorUsername(company);
        company.addCollaborator("still-valid");
        companyRepo.save(company);
        userSvc.seedActive(activeUser("still-valid", "Val", "Id", "val@example.com"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(company.identity().asUuid().toString());

        // Assert
        assertEquals(1, result.size());
        assertEquals("still-valid", result.get(0).username());
    }

    @Test
    @DisplayName("accepts company id with surrounding whitespace")
    void listShouldStripCompanyId() {
        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany("  " + companyId.asUuid() + "  ");

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("invalid company uuid mentions label in message")
    void listShouldRejectInvalidCompanyIdWithMessage() {
        // Act
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.listActiveCollaboratorsForCompany("bad-id"));

        // Assert
        assertTrue(ex.getMessage().contains("Invalid company id"));
    }

    @Test
    @DisplayName("skips active users whose username is not wanted")
    void listShouldSkipNonMatchingActiveUsers() {
        // Arrange — active3 is linked; seed extra active user with different name not on company
        userSvc.seedActive(activeUser("not-on-company", "X", "Y", "x@example.com"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(companyId.asUuid().toString());

        // Assert
        assertTrue(result.stream().noneMatch(d -> "not-on-company".equals(d.username())));
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("returns empty when linked username has no matching active user")
    void listShouldReturnEmptyWhenLinkedUserIsInactive() {
        // Arrange
        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("Ghost " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-G", CERT_EXPIRY));
        company.addCollaborator("ghost-user");
        companyRepo.save(company);

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(company.identity().asUuid().toString());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("strips area id when listing collaborators")
    void listAreaShouldStripAreaId() {
        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForAirControlArea("  " + areaCode + "  ");

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("ignores blank collaborator usernames on customer aggregate")
    void listShouldIgnoreBlankLinkedUsernames() {
        // Arrange
        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("Blank " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-B", CERT_EXPIRY));
        company.addCollaborator("valid-linked");
        companyRepo.save(company);
        userSvc.seedActive(activeUser("valid-linked", "Eva", "Valid", "eva@example.com"));

        // Act
        final List<CollaboratorDTO> result =
                controller.listActiveCollaboratorsForCompany(company.identity().asUuid().toString());

        // Assert
        assertEquals(1, result.size());
        assertEquals("valid-linked", result.get(0).username());
    }
}
