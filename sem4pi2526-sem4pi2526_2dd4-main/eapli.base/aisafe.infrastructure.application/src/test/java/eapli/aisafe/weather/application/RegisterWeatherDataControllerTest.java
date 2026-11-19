package eapli.aisafe.weather.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/** US041 — register meteorological observations for a known air control area. */
public class RegisterWeatherDataControllerTest {

    private static final Instant OBS = Instant.parse("2026-05-05T12:00:00Z");

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new RegisterWeatherDataController());
    }

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return "weather-user";
        }
    }

    private static final class TestAreaRepo extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
            implements AirControlAreaRepository {

        void addKnown(final String code) {
            save(new AirControlArea(code, "Area " + code, 40, 42, -9, -7));
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

    private static final class TestWeatherRepo extends InMemoryDomainRepository<WeatherData, WeatherDataId>
            implements WeatherDataRepository {

        WeatherData lastSaved;
        boolean returnPersistenceCopy;

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

        @Override
        public WeatherData save(final WeatherData entity) {
            lastSaved = entity;
            if (returnPersistenceCopy) {
                final WeatherData copy = new WeatherData(
                        entity.areaCode(),
                        entity.observedAt(),
                        entity.windSpeedMs(),
                        entity.windDirectionDegrees(),
                        entity.temperatureC(),
                        entity.humidityPercent());
                return super.save(copy);
            }
            return super.save(entity);
        }
    }

    private RegisterWeatherDataController controllerWithKnownLis() {
        final var auth = new FakeAuth();
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        return new RegisterWeatherDataController(auth, new TestWeatherRepo(), areas);
    }

    @Test
    public void registerShouldCheckRoleAndPersistWhenAreaKnown() {
        final var auth = new FakeAuth();
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var weather = new TestWeatherRepo();
        final var ctrl = new RegisterWeatherDataController(auth, weather, areas);

        final WeatherData saved = ctrl.register("pt-lis", OBS, 20, 50, 5, 180);

        assertArrayEquals(new Role[] { Roles.WEATHER_PERSON }, auth.lastRoles);
        assertSame(saved, weather.lastSaved);
        assertEquals("PT-LIS", saved.areaCode().toString());
        assertEquals(20, saved.temperatureC(), 0.001);
    }

    @Test
    public void registerShouldRejectUnknownArea() {
        final var ctrl = new RegisterWeatherDataController(new FakeAuth(), new TestWeatherRepo(), new TestAreaRepo());

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("XX-UNK", OBS, 20, 50, 5, 180));
    }

    @Test
    public void registerShouldRejectInvalidAreaCodeFormat() {
        final var ctrl = new RegisterWeatherDataController(new FakeAuth(), new TestWeatherRepo(), new TestAreaRepo());

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("INVALID", OBS, 20, 50, 5, 180));
    }

    @Test
    public void registerShouldRejectNullAreaCode() {
        final var ctrl = controllerWithKnownLis();

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register(null, OBS, 20, 50, 5, 180));
    }

    @Test
    public void registerShouldRejectBlankAreaCode() {
        final var ctrl = new RegisterWeatherDataController(new FakeAuth(), new TestWeatherRepo(), new TestAreaRepo());

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("", OBS, 20, 50, 5, 180));
        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("   ", OBS, 20, 50, 5, 180));
    }

    @Test
    public void registerShouldTrimAreaCodeWhitespace() {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var ctrl = new RegisterWeatherDataController(new FakeAuth(), new TestWeatherRepo(), areas);

        final WeatherData saved = ctrl.register("  pt-lis  ", OBS, 12, 60, 1, 45);

        assertEquals("PT-LIS", saved.areaCode().toString());
    }

    @Test
    public void registerShouldReturnWhatRepositorySaves() {
        final var auth = new FakeAuth();
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var weather = new TestWeatherRepo();
        weather.returnPersistenceCopy = true;
        final var ctrl = new RegisterWeatherDataController(auth, weather, areas);

        final WeatherData out = ctrl.register("PT-LIS", OBS, 20, 50, 5, 180);

        assertNotSame(weather.lastSaved, out);
        assertEquals("PT-LIS", out.areaCode().toString());
    }

    @Test
    public void registerShouldRejectNullObservedAt() {
        final var ctrl = controllerWithKnownLis();

        assertThrows(NullPointerException.class,
                () -> ctrl.register("PT-LIS", null, 20, 50, 5, 180));
    }

    @Test
    public void registerShouldRejectNegativeWindSpeed() {
        final var ctrl = controllerWithKnownLis();

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("PT-LIS", OBS, 20, 50, -0.1, 180));
    }

    @Test
    public void registerShouldRejectWindDirectionAtOrAbove360() {
        final var ctrl = controllerWithKnownLis();

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("PT-LIS", OBS, 20, 50, 5, 360));
    }

    @Test
    public void registerShouldRejectHumidityOutOfRange() {
        final var ctrl = controllerWithKnownLis();

        assertThrows(IllegalArgumentException.class,
                () -> ctrl.register("PT-LIS", OBS, 20, -0.01, 5, 180));
    }

    @Test
    public void registerShouldAcceptBoundaryValues() {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-OPO");
        final var ctrl = new RegisterWeatherDataController(new FakeAuth(), new TestWeatherRepo(), areas);

        final WeatherData a = ctrl.register("PT-OPO", OBS, 0, 0, 0, 0);
        assertEquals(0, a.windSpeedMs(), 0.0001);

        final WeatherData b = ctrl.register("PT-OPO", OBS, 1, 100, 0, 359.99);
        assertEquals(100, b.humidityPercent(), 0.0001);
    }
}
