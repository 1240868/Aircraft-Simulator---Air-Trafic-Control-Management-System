package eapli.aisafe.weather.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.fleet.application.UserStoryControllerPersistenceSupport;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.weather.application.importing.ImportParseException;
import eapli.aisafe.weather.application.importing.UnsupportedImportFormatException;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherSource;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/** US042 — bulk import weather observations. */
public class ImportWeatherDataControllerTest {

    private static final Instant OBS = Instant.parse("2026-05-29T14:00:00Z");

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ImportWeatherDataController());
    }

    @Test
    public void importShouldPersistValidRowsAndReportFailures() throws Exception {
        final var auth = new FakeAuth();
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        areas.addKnown("PT-OPO");
        final var weather = new TestWeatherRepo();
        final var ctrl = new ImportWeatherDataController(auth, weather, areas);

        final Path file = Files.createTempFile("weather-import", ".csv");
        final String csv = """
            areaCode;observedAt;temperatureC;humidityPercent;windSpeedMs;windDirectionDegrees;source
            PT-LIS;2026-05-29T14:00;18.5;62;5.2;270;METAR-Provider
            XX-UNK;2026-05-29T15:00;10;50;1;0;CSV-IMPORT
            PT-OPO;2026-05-29T16:00;12;55;2;45;
            """;
        Files.writeString(file, csv, StandardCharsets.UTF_8);

        final var result = ctrl.importFromFile("CSV", file);
        Files.deleteIfExists(file);

        assertArrayEquals(new Role[] { Roles.WEATHER_PERSON }, auth.lastRoles);
        assertEquals(3, result.totalRows());
        assertEquals(2, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertEquals(1, result.errors().size());
        assertEquals(2, weather.saved.size());
        assertEquals("METAR-Provider", weather.saved.get(0).source().value());
        assertEquals(WeatherSource.csvDefault().value(), weather.saved.get(1).source().value());
    }

    @Test
    public void importShouldRejectDuplicateObservation() throws Exception {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var weather = new TestWeatherRepo();
        weather.existing.add(new WeatherData(
                AirControlAreaCode.valueOf("PT-LIS"),
                OBS,
                1, 0, 10, 50,
                WeatherSource.csvDefault()));
        final var ctrl = new ImportWeatherDataController(new FakeAuth(), weather, areas);

        final Path file = Files.createTempFile("weather-dup", ".csv");
        Files.writeString(file, "PT-LIS;2026-05-29T14:00;18;60;5;180\n", StandardCharsets.UTF_8);

        final var result = ctrl.importFromFile("csv", file);
        Files.deleteIfExists(file);

        assertEquals(0, result.importedCount());
        assertEquals(1, result.rejectedCount());
    }

    @Test
    public void unsupportedFormatShouldFail() {
        final var ctrl = new ImportWeatherDataController(new FakeAuth(), new TestWeatherRepo(), new TestAreaRepo());
        assertThrows(UnsupportedImportFormatException.class,
                () -> ctrl.importFromFile("XML", Path.of("any.csv")));
    }

    @Test
    public void supportedFormatsShouldCheckRoleAndDelegate() {
        final var auth = new FakeAuth();
        final var ctrl = new ImportWeatherDataController(auth, new TestWeatherRepo(), new TestAreaRepo());

        final var formats = ctrl.supportedFormats();

        assertArrayEquals(new Role[] { Roles.WEATHER_PERSON }, auth.lastRoles);
        assertEquals(1, formats.size());
        assertEquals("CSV", formats.get(0));
    }

    @Test
    public void shouldAcceptServiceConstructor() {
        final var service = new ImportWeatherDataService(new TestWeatherRepo(), new TestAreaRepo());
        final var auth = new FakeAuth();
        final var ctrl = new ImportWeatherDataController(auth, service);

        final var formats = ctrl.supportedFormats();

        assertArrayEquals(new Role[] { Roles.WEATHER_PERSON }, auth.lastRoles);
        assertEquals(1, formats.size());
    }

    @Test
    public void emptyFileShouldFail() throws Exception {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var ctrl = new ImportWeatherDataController(new FakeAuth(), new TestWeatherRepo(), areas);
        final Path file = Files.createTempFile("weather-empty", ".csv");
        Files.writeString(file, "areaCode;observedAt;temperatureC;humidityPercent;windSpeedMs;windDirectionDegrees\n",
                StandardCharsets.UTF_8);
        assertThrows(ImportParseException.class, () -> ctrl.importFromFile("CSV", file));
        Files.deleteIfExists(file);
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

        final List<WeatherData> saved = new ArrayList<>();
        final List<WeatherData> existing = new ArrayList<>();

        @Override
        public WeatherData save(final WeatherData entity) {
            saved.add(entity);
            return super.save(entity);
        }

        @Override
        public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
            return List.copyOf(existing);
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                             final Instant fromInclusive,
                                                             final Instant toExclusive) {
            final List<WeatherData> result = new ArrayList<>();
            for (final WeatherData wd : existing) {
                if (wd.areaCode().equals(areaCode)
                        && !wd.observedAt().isBefore(fromInclusive)
                        && wd.observedAt().isBefore(toExclusive)) {
                    result.add(wd);
                }
            }
            for (final WeatherData wd : saved) {
                if (wd.areaCode().equals(areaCode)
                        && !wd.observedAt().isBefore(fromInclusive)
                        && wd.observedAt().isBefore(toExclusive)) {
                    result.add(wd);
                }
            }
            return result;
        }
    }
}
