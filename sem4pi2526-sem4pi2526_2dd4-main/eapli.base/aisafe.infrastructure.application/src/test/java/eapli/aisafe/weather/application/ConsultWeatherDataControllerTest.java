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
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherSource;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/** US043 — consult weather data by area and UTC calendar day. */
public class ConsultWeatherDataControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 29);

    @BeforeClass
    public static void configurePersistence() throws Exception {
        UserStoryControllerPersistenceSupport.ensureConfigured();
    }

    @Test
    public void defaultConstructorShouldWireDependencies() {
        assertNotNull(new ConsultWeatherDataController());
    }

    @Test
    public void consultShouldReturnRecordsSortedForUtcDay() {
        final var auth = new FakeAuth();
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var weather = new TestWeatherRepo();
        weather.seed(
                observation("PT-LIS", "2026-05-29T18:00:00Z", 20),
                observation("PT-LIS", "2026-05-29T08:00:00Z", 15),
                observation("PT-LIS", "2026-05-30T00:00:00Z", 99),
                observation("PT-OPO", "2026-05-29T12:00:00Z", 10));
        final var ctrl = new ConsultWeatherDataController(auth, weather, areas);

        final var result = ctrl.consult("pt-lis", DAY);

        assertArrayEquals(new Role[] {
                Roles.WEATHER_PERSON, Roles.PILOT, Roles.FLIGHT_CONTROL_OPERATOR
        }, auth.lastRoles);
        assertEquals("PT-LIS", result.areaCode());
        assertEquals(DAY, result.date());
        assertEquals(2, result.recordCount());
        assertEquals(Instant.parse("2026-05-29T08:00:00Z"), result.records().get(0).observedAt());
        assertEquals(Instant.parse("2026-05-29T18:00:00Z"), result.records().get(1).observedAt());
        assertEquals(15, result.records().get(0).temperatureC(), 0.001);
        assertEquals("CSV-IMPORT", result.records().get(0).source());
    }

    @Test
    public void consultShouldReturnEmptyWhenNoData() {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var ctrl = new ConsultWeatherDataController(new FakeAuth(), new TestWeatherRepo(), areas);

        final var result = ctrl.consult("PT-LIS", DAY);

        assertEquals(0, result.recordCount());
        assertEquals(0, result.records().size());
    }

    @Test
    public void consultShouldRejectUnknownArea() {
        final var ctrl = new ConsultWeatherDataController(new FakeAuth(), new TestWeatherRepo(), new TestAreaRepo());

        assertThrows(IllegalArgumentException.class, () -> ctrl.consult("XX-UNK", DAY));
    }

    @Test
    public void shouldAcceptServiceConstructor() {
        final var areas = new TestAreaRepo();
        areas.addKnown("PT-LIS");
        final var service = new ConsultWeatherDataService(new TestWeatherRepo(), areas);
        final var ctrl = new ConsultWeatherDataController(new FakeAuth(), service);

        final var result = ctrl.consult("PT-LIS", DAY);

        assertEquals(0, result.recordCount());
    }

    private static WeatherData observation(final String area, final String instant, final double temp) {
        return new WeatherData(
                AirControlAreaCode.valueOf(area),
                Instant.parse(instant),
                5, 180, temp, 50,
                WeatherSource.csvDefault());
    }

    private static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return "consult-user";
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

        private final List<WeatherData> data = new ArrayList<>();

        void seed(final WeatherData... items) {
            for (final WeatherData wd : items) {
                data.add(wd);
                save(wd);
            }
        }

        @Override
        public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
            return List.copyOf(data);
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                             final Instant fromInclusive,
                                                             final Instant toExclusive) {
            final List<WeatherData> result = new ArrayList<>();
            for (final WeatherData wd : data) {
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
