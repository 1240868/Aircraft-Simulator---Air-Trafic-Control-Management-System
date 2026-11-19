package eapli.aisafe.company.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.aircontrol.application.RegisterAreaCollaboratorDTO;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
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
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AddCollaboratorController")
class AddCollaboratorControllerTest {

    private static final LocalDate CERT_EXPIRY = LocalDate.of(2032, 6, 15);

    private static String lettersOnly2() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "AB").substring(0, 2);
    }

    private static String lettersOnly3() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "ABC").substring(0, 3);
    }

    private static SystemUser user(final String username, final String email) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "First", "Last", email)
                .withRoles(Roles.BACKOFFICE_OPERATOR)
                .build();
    }

    private static SystemUser userWithoutEmail(final String username) throws Exception {
        final SystemUser built = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "First", "Last", "placeholder@example.com")
                .withRoles(Roles.BACKOFFICE_OPERATOR)
                .build();
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

    private static final class RecordingAuthz extends AuthorizationService {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }
    }

    private static final class RecordingUserService extends UserManagementService {
        private final List<SystemUser> all = new ArrayList<>();
        int registerCalls;
        Set<Role> lastRoles;

        RecordingUserService() {
            super(new InMemoryUserRepository(), new NilPasswordPolicy(), new PlainTextEncoder());
        }

        void seedExisting(final SystemUser u) {
            all.add(u);
        }

        @Override
        public SystemUser registerNewUser(final String username,
                                          final String password,
                                          final String firstName,
                                          final String lastName,
                                          final String email,
                                          final Set<Role> roles,
                                          final Calendar createdOn) {
            registerCalls++;
            lastRoles = Set.copyOf(roles);
            final SystemUser created = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                    .with(username, password, firstName, lastName, email)
                    .withRoles(roles.toArray(Role[]::new))
                    .build();
            all.add(created);
            return created;
        }

        @Override
        public Iterable<SystemUser> allUsers() {
            return List.copyOf(all);
        }
    }

    private static final class RecordingCompanyRepo
            extends InMemoryDomainRepository<AirTransportCompany, CompanyId>
            implements AirTransportCompanyRepository {

        int saveCalls;

        @Override
        public AirTransportCompany save(final AirTransportCompany entity) {
            saveCalls++;
            return super.save(entity);
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

    private static final class RecordingAreaRepo
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
            implements AirControlAreaRepository {

        int saveCalls;

        @Override
        public AirControlArea save(final AirControlArea entity) {
            saveCalls++;
            return super.save(entity);
        }

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
    private RecordingCompanyRepo companyRepo;
    private RecordingAreaRepo areaRepo;
    private ListCollaboratorsController listController;
    private AddCollaboratorController controller;
    private CompanyId companyId;
    private AirControlAreaCode areaCode;

    @BeforeAll
    static void configurePersistenceFactory() throws Exception {
        ListCollaboratorsControllerTest.ensureTestRepositoryFactory();
    }

    @BeforeEach
    void setUp() {
        authz = new RecordingAuthz();
        userSvc = new RecordingUserService();
        companyRepo = new RecordingCompanyRepo();
        areaRepo = new RecordingAreaRepo();
        listController = new ListCollaboratorsController(authz, userSvc, companyRepo, areaRepo);
        controller = new AddCollaboratorController(authz, userSvc, listController, companyRepo, areaRepo);

        final AirTransportCompany company = new AirTransportCompany(
                new CompanyName("SkyJet " + lettersOnly2()),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-1", CERT_EXPIRY));
        companyRepo.save(company);
        companyId = company.identity();

        final AirControlArea area = new AirControlArea("PT-LIS", "Lisboa FIR", 36.0, 42.0, -10.0, -6.0);
        areaRepo.save(area);
        areaCode = area.identity();

        companyRepo.saveCalls = 0;
        areaRepo.saveCalls = 0;
        userSvc.registerCalls = 0;
    }

    @Test
    @DisplayName("happy path: register company collaborator with correct role and persist link")
    void registerCompanyCollaboratorShouldSucceed() {
        // Arrange
        final String username = "collab-co-" + UUID.randomUUID().toString().substring(0, 8);
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                username,
                "duMMy1",
                "ana.silva@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act
        final SystemUser created = controller.registerCollaboratorForAirTransportCompany(dto);

        // Assert
        assertNotNull(created);
        assertEquals(username, created.username().toString());
        assertArrayEquals(new Role[] {Roles.ADMIN, Roles.BACKOFFICE_OPERATOR}, authz.lastRoles);
        assertEquals(Set.of(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR), userSvc.lastRoles);
        assertEquals(1, userSvc.registerCalls);
        assertEquals(1, companyRepo.saveCalls, "company aggregate should be persisted after linking collaborator");
        final AirTransportCompany saved = companyRepo.ofIdentity(companyId).orElseThrow();
        assertTrue(saved.collaboratorUsernames().contains(username));
    }

    @Test
    @DisplayName("happy path: register area collaborator with correct role and persist link")
    void registerAreaCollaboratorShouldSucceed() {
        // Arrange
        final String username = "collab-area-" + UUID.randomUUID().toString().substring(0, 8);
        final var dto = new RegisterAreaCollaboratorDTO(
                areaCode.toString(),
                username,
                "duMMy1",
                "bob.costa@example.com",
                "Bob",
                "Costa",
                "+351987654321");

        // Act
        final SystemUser created = controller.registerCollaboratorForAirControlArea(dto);

        // Assert
        assertNotNull(created);
        assertEquals(Set.of(Roles.AIR_CONTROL_AREA_COLLABORATOR), userSvc.lastRoles);
        assertEquals(1, areaRepo.saveCalls);
        final AirControlArea saved = areaRepo.ofIdentity(areaCode).orElseThrow();
        assertTrue(saved.collaboratorUsernames().contains(username));
    }

    @Test
    @DisplayName("rejects invalid email from domain validation")
    void registerShouldRejectInvalidEmail() {
        // Arrange
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                "user1",
                "duMMy1",
                "not-an-email",
                "Ana",
                "Silva",
                "+351912345678");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
        assertEquals(0, companyRepo.saveCalls);
    }

    @Test
    @DisplayName("rejects duplicate email already registered")
    void registerShouldRejectDuplicateEmail() {
        // Arrange
        userSvc.seedExisting(user("existing", "dup@example.com"));
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                "newuser",
                "duMMy1",
                "dup@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act & Assert
        assertThrows(IntegrityViolationException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
        assertEquals(0, userSvc.registerCalls);
    }

    @Test
    @DisplayName("rejects unknown company id")
    void registerShouldRejectMissingCompany() {
        // Arrange
        final var dto = new RegisterCustomerCollaboratorDTO(
                UUID.randomUUID().toString(),
                "user2",
                "duMMy1",
                "new@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act & Assert
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
        assertTrue(ex.getMessage().contains("Air transport company not found"));
    }

    @Test
    @DisplayName("rejects malformed company id")
    void registerShouldRejectInvalidCompanyId() {
        // Arrange
        final var dto = new RegisterCustomerCollaboratorDTO(
                "not-a-uuid",
                "user3",
                "duMMy1",
                "valid@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
    }

    @Test
    @DisplayName("rejects unknown air control area")
    void registerShouldRejectMissingArea() {
        // Arrange
        final var dto = new RegisterAreaCollaboratorDTO(
                "XX-UNK",
                "areauser",
                "duMMy1",
                "area@example.com",
                "Bob",
                "Costa",
                "+351987654321");

        // Act & Assert
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirControlArea(dto));
        assertTrue(ex.getMessage().contains("Air control area not found"));
    }

    @Test
    @DisplayName("rejects duplicate email on area registration")
    void registerAreaShouldRejectDuplicateEmail() {
        // Arrange
        userSvc.seedExisting(user("existing-area", "area.dup@example.com"));
        final var dto = new RegisterAreaCollaboratorDTO(
                areaCode.toString(),
                "new-area-user",
                "duMMy1",
                "area.dup@example.com",
                "Bob",
                "Costa",
                "+351987654321");

        // Act & Assert
        assertThrows(IntegrityViolationException.class,
                () -> controller.registerCollaboratorForAirControlArea(dto));
        assertEquals(0, areaRepo.saveCalls);
    }

    @Test
    @DisplayName("detects duplicate email ignoring case")
    void registerShouldRejectDuplicateEmailIgnoringCase() {
        // Arrange
        userSvc.seedExisting(user("existing", "Registered@Example.COM"));
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                "newuser2",
                "duMMy1",
                "registered@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act & Assert
        assertThrows(IntegrityViolationException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
    }

    @Test
    @DisplayName("does not treat null-email users as duplicate")
    void registerShouldIgnoreNullEmailWhenCheckingDuplicates() throws Exception {
        // Arrange
        userSvc.seedExisting(userWithoutEmail("no-email-user"));
        final String username = "fresh-" + UUID.randomUUID().toString().substring(0, 8);
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                username,
                "duMMy1",
                "unique@example.com",
                "Ana",
                "Silva",
                "+351912345678");

        // Act
        final SystemUser created = controller.registerCollaboratorForAirTransportCompany(dto);

        // Assert
        assertEquals(username, created.username().toString());
        assertEquals(1, userSvc.registerCalls);
    }

    @Test
    @DisplayName("strips whitespace from company id and person fields")
    void registerShouldStripInputFields() {
        // Arrange
        final String username = "strip-" + UUID.randomUUID().toString().substring(0, 8);
        final var dto = new RegisterCustomerCollaboratorDTO(
                "  " + companyId.asUuid().toString() + "  ",
                username,
                "duMMy1",
                "  strip@example.com  ",
                "  Ana  ",
                "  Silva  ",
                "+351912345678");

        // Act
        final SystemUser created = controller.registerCollaboratorForAirTransportCompany(dto);

        // Assert
        assertEquals(username, created.username().toString());
        assertEquals("Ana", created.name().firstName());
        assertEquals("Silva", created.name().lastName());
    }

    @Test
    @DisplayName("rejects invalid area id format")
    void registerAreaShouldRejectInvalidAreaId() {
        // Arrange
        final var dto = new RegisterAreaCollaboratorDTO(
                "   ",
                "areauser2",
                "duMMy1",
                "area2@example.com",
                "Bob",
                "Costa",
                "+351987654321");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirControlArea(dto));
    }

    @Test
    @DisplayName("rejects blank first name on area path")
    void registerAreaShouldRejectBlankFirstName() {
        // Arrange
        final var dto = new RegisterAreaCollaboratorDTO(
                areaCode.toString(),
                "badname",
                "duMMy1",
                "valid@example.com",
                "   ",
                "Costa",
                "+351987654321");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirControlArea(dto));
    }

    @Test
    @DisplayName("delegates area catalog to list controller")
    void listAreasShouldDelegate() {
        // Act
        boolean found = false;
        for (final var option : controller.listAirControlAreasForSelection()) {
            if (areaCode.toString().equals(option.areaId())) {
                found = true;
                break;
            }
        }

        // Assert
        assertTrue(found);
    }

    @Test
    @DisplayName("default constructor wires production dependencies")
    void defaultConstructorShouldConstruct() {
        assertNotNull(new AddCollaboratorController());
    }

    @Test
    @DisplayName("rejects invalid phone on company registration")
    void registerShouldRejectInvalidPhone() {
        final var dto = new RegisterCustomerCollaboratorDTO(
                companyId.asUuid().toString(),
                "bad-phone",
                "duMMy1",
                "phone@example.com",
                "Ana",
                "Silva",
                "not-a-phone");

        assertThrows(IllegalArgumentException.class,
                () -> controller.registerCollaboratorForAirTransportCompany(dto));
        assertEquals(0, userSvc.registerCalls);
    }

    @Test
    @DisplayName("delegates company catalog to list controller")
    void listCompaniesShouldDelegate() {
        // Act
        boolean found = false;
        for (final AirTransportCompanyOptionDTO option : controller.listAirTransportCompaniesForSelection()) {
            if (companyId.asUuid().toString().equals(option.companyId())) {
                found = true;
                break;
            }
        }

        // Assert
        assertTrue(found);
    }
}
