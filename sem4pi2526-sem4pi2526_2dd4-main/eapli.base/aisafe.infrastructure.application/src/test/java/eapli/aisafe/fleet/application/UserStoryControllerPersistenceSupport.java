package eapli.aisafe.fleet.application;

import eapli.aisafe.App;
import eapli.aisafe.Settings;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.FakePendingFlights;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestAircraftRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestModelRepo;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Properties;

/**
 * Configures an in-memory {@link RepositoryFactory} so default constructors on US controllers
 * can be exercised without persistence.impl.
 */
public final class UserStoryControllerPersistenceSupport {

    private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";

    private static boolean authzConfigured;

    static {
        configureFactoryProperty();
    }

    private UserStoryControllerPersistenceSupport() {
    }

    private static void configureFactoryProperty() {
        try {
            final Field settingsField = App.class.getDeclaredField("SETTINGS");
            settingsField.setAccessible(true);
            final Settings settings = (Settings) settingsField.get(null);
            final Field propsField = Settings.class.getDeclaredField("props");
            propsField.setAccessible(true);
            final Properties props = (Properties) propsField.get(settings);
            props.setProperty(REPOSITORY_FACTORY_KEY,
                    UserStoryControllerPersistenceSupport.class.getName() + "$TestRepositoryFactory");
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Must run before any {@link PersistenceContext} access (it caches the factory at class load).
     */
    public static void ensureConfigured() throws Exception {
        configureFactoryProperty();
        resetInMemoryData();

        if (!authzConfigured) {
            AuthzRegistry.configure(
                    PersistenceContext.repositories().users(),
                    new NilPasswordPolicy(),
                    new PlainTextEncoder());
            authzConfigured = true;
        }
    }

    private static void resetInMemoryData() throws Exception {
        final Class<?> repoClass = Class.forName(
                "eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryRepository");
        final Method reset = repoClass.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    public static final class TestRepositoryFactory implements RepositoryFactory {

        private final InMemoryUserRepository users = new InMemoryUserRepository();
        private final TestCompanyRepo companies = new TestCompanyRepo();
        private final TestModelRepo models = new TestModelRepo();
        private final TestAircraftRepo aircraft = new TestAircraftRepo();
        private final TestAreaRepo areas = new TestAreaRepo();
        private final TestWeatherRepo weather = new TestWeatherRepo();
        private final FakePendingFlights pending = new FakePendingFlights();
        private final TestPilotRepo pilots = new TestPilotRepo();
        private final TestAirportRepo airportsForFactory = new TestAirportRepo();

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
        public AirportRepository airports() {
            return airportsForFactory;
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
        public EngineModelRepository engineModels() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WeatherDataRepository weatherData() {
            return weather;
        }

        @Override
        public PendingFlightsChecker pendingFlightsChecker() {
            return pending;
        }

        @Override
        public PilotRepository pilots() {
            return pilots;
        }

        @Override
        public FlightPlanRepository flightPlans() {
            return null;
        }

        @Override
        public FlightRouteRepository flightRoutes() {
            return null;
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

    private static final class TestPilotRepo extends InMemoryDomainRepository<Pilot, PilotId>
            implements PilotRepository {

        @Override
        public Optional<Pilot> findByEmail(final Email email) {
            for (final Pilot p : findAll()) {
                if (p.getEmail().value().equals(email.value())) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }

        @Override
        public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
            return List.of();
        }

        @Override
        public Optional<Pilot> ofIdentity(final String id) {
            final PilotId pid = new PilotId(UUID.fromString(id));
            return matchOne(p -> p.identity().equals(pid));
        }
    }

    private static final class TestWeatherRepo extends InMemoryDomainRepository<WeatherData, WeatherDataId>
            implements WeatherDataRepository {

        @Override
        public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
            return List.of();
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                             final Instant fromInclusive,
                                                             final Instant toExclusive) {
            return List.of();
        }
    }

    private static final class TestAirportRepo extends InMemoryDomainRepository<Airport, String>
            implements AirportRepository {

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return ofIdentity(icaoCode.trim().toUpperCase());
        }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Optional<Airport> findById(final String id) {
            return ofIdentity(id);
        }

        @Override
        public Boolean exists(final AirportId id) {
            return ofIdentity(id.toString()).isPresent();
        }
    }
}
