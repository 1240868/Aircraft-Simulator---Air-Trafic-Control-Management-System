package eapli.aisafe.company.application;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

public class RegisterAirTransportCompanyControllerTest {

    private static final LocalDate CERT_EXPIRY = LocalDate.of(2032, 6, 15);

    private static final AtomicInteger CODE_SEQ = new AtomicInteger(0);

    private static String lettersOnly2() {
        final int n = CODE_SEQ.incrementAndGet();
        return "" + (char) ('A' + (n / 26) % 26) + (char) ('A' + n % 26);
    }

    private static String lettersOnly3() {
        final int n = CODE_SEQ.incrementAndGet();
        return ""
                + (char) ('A' + (n / (26 * 26)) % 26)
                + (char) ('A' + (n / 26) % 26)
                + (char) ('A' + n % 26);
    }

    private static final class FakeAuth implements AuthorizationPort {
        Role[] last;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.last = roles;
        }

        @Override
        public String currentUsername() {
            return "bo-op";
        }
    }

    /** Repositório isolado por teste (evita estado global do {@code InMemoryDomainRepository}). */
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
            final String key = iataCode.trim().toUpperCase();
            for (final var c : store.values()) {
                if (c.iataCode().value().equals(key)) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            final String key = icaoCode.trim().toUpperCase();
            for (final var c : store.values()) {
                if (c.icaoCode().value().equals(key)) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            final String u = systemUsername == null ? "" : systemUsername.strip();
            if (u.isEmpty()) {
                return Optional.empty();
            }
            for (final var c : store.values()) {
                if (c.collaboratorUsernames().contains(u)) {
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
            for (final var c : store.values()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }
    }

    private TestCompanyRepo repo;
    private FakeAuth auth;
    private RegisterAirTransportCompanyController controller;

    @Before
    public void setUp() {
        repo = new TestCompanyRepo();
        auth = new FakeAuth();
        controller = new RegisterAirTransportCompanyController(auth, repo);
    }

    private static int count(final TestCompanyRepo r) {
        int n = 0;
        for (final AirTransportCompany ignored : r.findAll()) {
            n++;
        }
        return n;
    }

    @Test
    public void registerShouldPersistAndCheckRoles() {
        final int before = count(repo);
        final String iata = lettersOnly2();
        final String icao = lettersOnly3();
        final AirTransportCompany c = controller.register(
                "SkyJet " + iata,
                iata,
                icao,
                "AOC-" + iata,
                CERT_EXPIRY);

        assertNotNull(c.identity());
        assertEquals(iata, c.iataCode().value());
        assertEquals(icao, c.icaoCode().value());
        assertArrayEquals(new Role[] { Roles.ADMIN, Roles.BACKOFFICE_OPERATOR }, auth.last);
        assertEquals(before + 1, count(repo));
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerShouldRejectDuplicateIata() {
        final String iata = lettersOnly2();
        final String icao1 = lettersOnly3();
        final String icao2 = lettersOnly3();
        controller.register("First " + iata, iata, icao1, "A1", CERT_EXPIRY);
        controller.register("Second " + iata, iata, icao2, "A2", CERT_EXPIRY);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerShouldRejectDuplicateIcao() {
        final String iata1 = lettersOnly2();
        final String iata2 = lettersOnly2();
        final String icao = lettersOnly3();
        controller.register("First " + iata1, iata1, icao, "A1", CERT_EXPIRY);
        controller.register("Second " + iata2, iata2, icao, "A2", CERT_EXPIRY);
    }

    @Test(expected = IntegrityViolationException.class)
    public void registerShouldRejectDuplicateIataIgnoringCase() {
        final String iata = lettersOnly2();
        final String icao1 = lettersOnly3();
        final String icao2 = lettersOnly3();
        controller.register("Co A " + iata, iata, icao1, "A1", CERT_EXPIRY);
        controller.register("Co B " + iata, iata.toLowerCase(), icao2, "A2", CERT_EXPIRY);
    }

    @Test
    public void registerShouldAllowDifferentIataAndIcao() {
        final int before = count(repo);
        final String iata1 = lettersOnly2();
        final String iata2 = lettersOnly2();
        final String icao1 = lettersOnly3();
        final String icao2 = lettersOnly3();
        final var a = controller.register("Alpha " + iata1, iata1, icao1, "C1", CERT_EXPIRY);
        final var b = controller.register("Beta " + iata2, iata2, icao2, "C2", CERT_EXPIRY);
        assertEquals(before + 2, count(repo));
        assertEquals(iata1, a.iataCode().value());
        assertEquals(iata2, b.iataCode().value());
    }
}
