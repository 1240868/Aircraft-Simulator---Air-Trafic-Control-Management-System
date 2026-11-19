package eapli.aisafe.fleet.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.App;
import eapli.aisafe.Settings;
import eapli.aisafe.application.security.AuthorizationPort;
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
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AddAircraftController")
class AddAircraftControllerTest {

    private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";
    private static final LocalDate MFG = LocalDate.of(2012, 3, 3);
    private static final LocalDate FIRST_REG = LocalDate.of(2012, 6, 1);

    private static boolean persistenceFactoryConfigured;
    private static boolean authzConfigured;

    private static final AtomicInteger UNIQUE_REG_SEQ = new AtomicInteger(10_000);

    private static String uniqueRegistration() {
        return "CS-" + UNIQUE_REG_SEQ.incrementAndGet();
    }

    private static String lettersOnly2() {
        final int n = UNIQUE_REG_SEQ.incrementAndGet();
        return "" + (char) ('A' + (n / 26) % 26) + (char) ('A' + n % 26);
    }

    private static String lettersOnly3() {
        final int n = UNIQUE_REG_SEQ.incrementAndGet();
        return ""
                + (char) ('A' + (n / (26 * 26)) % 26)
                + (char) ('A' + (n / 26) % 26)
                + (char) ('A' + n % 26);
    }

    static void ensureFleetTestRepositoryFactory() throws Exception {
        if (!persistenceFactoryConfigured) {
            final Field settingsField = App.class.getDeclaredField("SETTINGS");
            settingsField.setAccessible(true);
            final Settings settings = (Settings) settingsField.get(null);
            final Field propsField = Settings.class.getDeclaredField("props");
            propsField.setAccessible(true);
            final Properties props = (Properties) propsField.get(settings);
            props.setProperty(REPOSITORY_FACTORY_KEY,
                    AddAircraftControllerTest.class.getName() + "$FleetTestRepositoryFactory");
            persistenceFactoryConfigured = true;
        }
        if (!authzConfigured) {
            AuthzRegistry.configure(
                    PersistenceContext.repositories().users(),
                    new NilPasswordPolicy(),
                    new PlainTextEncoder());
            authzConfigured = true;
        }
    }

    /**
     * In-memory {@link RepositoryFactory} for default-constructor wiring (no persistence.impl).
     */
    public static final class FleetTestRepositoryFactory implements RepositoryFactory {
        private final InMemoryUserRepository users = new InMemoryUserRepository();
        private final TestCompanyRepo companies = new TestCompanyRepo();
        private final TestModelRepo models = new TestModelRepo();
        private final TestAircraftRepo aircraft = new TestAircraftRepo();

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
            return new TestAreaRepo();
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
        public AircraftModelRepository aircraftModels() {
            return models;
        }

        @Override
        public AircraftRepository aircraft() {
            return aircraft;
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

    private static final class FakeAuth implements AuthorizationPort {
        Role[] last;
        String username = "collab1";

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.last = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    private static final class TestAircraftRepo implements AircraftRepository {

        private final Map<RegistrationNumber, Aircraft> store = new HashMap<>();

        @Override
        public Aircraft save(final Aircraft entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Aircraft> ofIdentity(final RegistrationNumber id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<Aircraft> findAll() {
            return store.values();
        }

        @Override
        public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
            return ofIdentity(registration);
        }

        @Override
        public Optional<Aircraft> findByRegistration(final String registration) {
            for (final Aircraft a : store.values()) {
                if (a.identity().toString().equals(registration)) {
                    return Optional.of(a);
                }
            }
            return Optional.empty();
        }

        @Override
        public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
            final List<Aircraft> out = new ArrayList<>();
            for (final Aircraft a : store.values()) {
                if (a.ownerCompanyId().equals(companyId)) {
                    out.add(a);
                }
            }
            return out;
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                                   final AircraftModelId modelId) {
            return List.of();
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                               final int passengerCapacity) {
            return List.of();
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndAge(final CompanyId companyId,
                                                               final int completeYears,
                                                               final LocalDate asOf) {
            return List.of();
        }

        @Override
        public void delete(final Aircraft entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final RegistrationNumber id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private static final class TestModelRepo extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                       final String manufacturer) {
            return Optional.empty();
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            final AircraftModelId mid = new AircraftModelId(UUID.fromString(id));
            return matchOne(e -> e.identity().equals(mid));
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            return matchOne(e -> e.identity().equals(id));
        }
    }

    private static final class TestAreaRepo extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
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
        public long count() {
            return store.size();
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            for (final AirTransportCompany c : store.values()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }
    }

    private TestAircraftRepo aircraft;
    private TestModelRepo models;
    private TestCompanyRepo companies;
    private FakeAuth auth;
    private AddAircraftController controller;
    private CompanyId companyId;
    private AircraftModelId modelId;
    private String collaboratorUsername;

    @BeforeAll
    static void configurePersistenceForDefaultConstructor() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @BeforeEach
    void setUp() {
        aircraft = new TestAircraftRepo();
        models = new TestModelRepo();
        companies = new TestCompanyRepo();
        auth = new FakeAuth();
        collaboratorUsername = "collab-" + UUID.randomUUID().toString().substring(0, 12);
        auth.username = collaboratorUsername;
        controller = new AddAircraftController(auth, aircraft, models, companies);

        final var company = new AirTransportCompany(
                new CompanyName("TAP Air"),
                new CompanyIataCode(lettersOnly2()),
                new CompanyIcaoCode(lettersOnly3()),
                new AirOperatorCertificate("AOC-1", LocalDate.now().plusYears(1)));
        company.addCollaborator(collaboratorUsername);
        companies.save(company);
        companyId = company.identity();

        final var model = new AircraftModel(
                "A320-" + UUID.randomUUID().toString().substring(0, 6),
                "Airbus",
                AircraftType.PASSENGER,
                EngineType.TURBOFAN,
                2,
                180,
                6000,
                List.of(EngineModelId.newId()));
        models.save(model);
        modelId = model.identity();
    }

    @Test
    @DisplayName("lists aircraft models as selection DTOs with all fields")
    void listAircraftModelsBuildsDtos() {
        final List<AircraftModelOptionDTO> dtos = controller.listAircraftModels();

        final AircraftModelOptionDTO match = dtos.stream()
                .filter(d -> d.modelId().equals(modelId.toString()))
                .findFirst()
                .orElseThrow();
        assertEquals("A320", match.modelName().substring(0, 4));
        assertEquals("Airbus", match.manufacturer());
        assertEquals(AircraftType.PASSENGER.name(), match.type());
        assertEquals(180, match.passengerCapacity());
        assertArrayEquals(new Role[] {Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR}, auth.last);
    }

    @Test
    @DisplayName("lists every persisted model")
    void listAircraftModelsIncludesAllPersistedModels() {
        final var second = new AircraftModel(
                "B737-" + UUID.randomUUID().toString().substring(0, 6),
                "Boeing",
                AircraftType.PASSENGER,
                EngineType.TURBOFAN,
                2,
                160,
                5500,
                List.of(EngineModelId.newId()));
        models.save(second);

        final List<AircraftModelOptionDTO> dtos = controller.listAircraftModels();

        assertTrue(dtos.stream().anyMatch(d -> d.modelId().equals(modelId.toString())));
        assertTrue(dtos.stream().anyMatch(d -> d.modelId().equals(second.identity().toString())));
    }

    @Test
    @DisplayName("resolves company of authenticated collaborator")
    void resolveCompanyReturnsLinkedCompany() {
        assertEquals(companyId, controller.resolveCompanyOfAuthenticatedUser());
        assertArrayEquals(new Role[] {Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR}, auth.last);
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
    @DisplayName("happy path: persists aircraft for collaborator company")
    void addAircraftPersists() {
        final String reg = uniqueRegistration();
        final int before = countAircraft();

        final Aircraft a = controller.addAircraft(
                reg, "PT", modelId.toString(),
                List.of(new CabinSeatBlock("ECONOMY", 100)), MFG, null);

        assertEquals(RegistrationNumber.valueOf(reg), a.identity());
        assertEquals(companyId, a.ownerCompanyId());
        assertEquals(modelId, a.aircraftModelId());
        assertEquals(before + 1, countAircraft());
        assertArrayEquals(new Role[] {Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR}, auth.last);
    }

    @Test
    @DisplayName("accepts both manufacturing and first registration dates")
    void addAircraftAcceptsBothDates() {
        final Aircraft a = controller.addAircraft(
                uniqueRegistration(), "PT", modelId.toString(),
                List.of(new CabinSeatBlock("ECONOMY", 50)), MFG, FIRST_REG);

        assertNotNull(a);
    }

    @Test
    @DisplayName("accepts multiple cabin classes within model capacity")
    void addAircraftAcceptsMultipleCabinBlocks() {
        final Aircraft a = controller.addAircraft(
                uniqueRegistration(), "PT", modelId.toString(),
                List.of(
                        new CabinSeatBlock("BUSINESS", 20),
                        new CabinSeatBlock("ECONOMY", 140)),
                MFG, null);

        assertEquals(160, a.totalConfiguredSeats());
    }

    @Test
    @DisplayName("strips whitespace around model id")
    void addAircraftAcceptsWhitespaceAroundModelId() {
        final int before = countAircraft();

        final Aircraft a = controller.addAircraft(
                uniqueRegistration(), "PT", "  " + modelId.toString() + "  ",
                List.of(new CabinSeatBlock("ECONOMY", 60)), null, FIRST_REG);

        assertNotNull(a);
        assertEquals(before + 1, countAircraft());
    }

    @Test
    @DisplayName("rejects duplicate registration number")
    void addAircraftFailsWhenDuplicateRegistration() {
        final String reg = uniqueRegistration();
        controller.addAircraft(reg, "PT", modelId.toString(),
                List.of(new CabinSeatBlock("ECONOMY", 50)), MFG, null);

        final IntegrityViolationException ex = assertThrows(IntegrityViolationException.class,
                () -> controller.addAircraft(reg, "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 50)), MFG, null));
        assertTrue(ex.getMessage().contains(reg));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("rejects unknown aircraft model")
    void addAircraftFailsWhenModelMissing() {
        final String missingId = UUID.randomUUID().toString();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("CS-XXX", "PT", missingId,
                        List.of(new CabinSeatBlock("ECONOMY", 50)), MFG, null));
        assertTrue(ex.getMessage().contains("Aircraft model not found"));
        assertTrue(ex.getMessage().contains(missingId));
    }

    @Test
    @DisplayName("rejects malformed model id")
    void addAircraftFailsWhenModelIdInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("CS-YYY", "PT", "not-a-uuid",
                        List.of(new CabinSeatBlock("ECONOMY", 10)), MFG, null));
    }

    @Test
    @DisplayName("rejects when both dates are null")
    void addAircraftFailsWhenBothDatesNull() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("CS-QQQ", "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 10)), null, null));
        assertTrue(ex.getMessage().contains("Manufacturing date or first registration date is required"));
    }

    @Test
    @DisplayName("rejects cabin seats above model capacity (US070 invariant)")
    void addAircraftFailsWhenSeatsExceedCapacity() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 181)), MFG, null));
        assertTrue(ex.getMessage().contains("cannot exceed model capacity"));
    }

    @Test
    @DisplayName("rejects empty cabin allocation list")
    void addAircraftFailsWhenNoCabinBlocks() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PT", modelId.toString(),
                        Collections.emptyList(), MFG, null));
    }

    @Test
    @DisplayName("rejects zero total seats")
    void addAircraftFailsWhenZeroSeats() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 0)), MFG, null));
    }

    @Test
    @DisplayName("rejects invalid registration number format")
    void addAircraftFailsWhenRegistrationInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft("!!", "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 10)), MFG, null));
    }

    @Test
    @DisplayName("rejects invalid country code")
    void addAircraftFailsWhenCountryInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PORTUGAL", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 10)), MFG, null));
    }

    @Test
    @DisplayName("rejects manufacturing date in the future")
    void addAircraftFailsWhenManufacturingDateInFuture() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 10)),
                        LocalDate.now().plusDays(1), null));
    }

    @Test
    @DisplayName("rejects first registration date in the future")
    void addAircraftFailsWhenFirstRegistrationInFuture() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.addAircraft(uniqueRegistration(), "PT", modelId.toString(),
                        List.of(new CabinSeatBlock("ECONOMY", 10)),
                        null, LocalDate.now().plusDays(1)));
    }

    @Test
    @DisplayName("normalizes registration number casing")
    void addAircraftNormalizesRegistration() {
        final String regLower = uniqueRegistration().toLowerCase();

        final Aircraft a = controller.addAircraft(
                regLower, "pt", modelId.toString(),
                List.of(new CabinSeatBlock("ECONOMY", 40)), MFG, null);

        assertEquals(RegistrationNumber.valueOf(regLower), a.identity());
    }

    private int countAircraft() {
        int n = 0;
        for (final Aircraft ignored : aircraft.findAll()) {
            n++;
        }
        return n;
    }
}
