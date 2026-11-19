package eapli.aisafe.airport.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class RegisterAirportControllerTest {

    private static final class FakeAuth implements AuthorizationPort {
        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            // unit tests: no registry session required
        }

        @Override
        public String currentUsername() {
            return "test-operator";
        }
    }

    private static final class TestAirportRepository extends InMemoryDomainRepository<Airport, String>
            implements AirportRepository {

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return matchOne(e -> e.icaoCode().equalsIgnoreCase(icaoCode));
        }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return matchOne(e -> e.iataCode().equalsIgnoreCase(iataCode));
        }

        @Override
        public Boolean exists(final AirportId id) {
            return ofIdentity(id.toString()).isPresent();
        }
    }

    private static final class TestAirControlAreaRepository
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
            implements AirControlAreaRepository {

    @Override
    public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
        return matchOne(e -> e.identity().equals(code));
    }

    @Override
    public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
        return Optional.empty();
    }
}

    private TestAirportRepository airports;
    private TestAirControlAreaRepository areas;
    private RegisterAirportController controller;

    @Before
    public void setUp() {
        airports = new TestAirportRepository();
        areas = new TestAirControlAreaRepository();
        areas.save(new AirControlArea("PT-LIS", "Lisboa FIR", 36.0, 43.0, -12.0, -6.0));
        controller = new RegisterAirportController(new FakeAuth(), airports, areas);
    }

    @Test
    public void ensureRegisterAirportReturnsAirport() {
        final Airport airport = controller.registerAirport(
                "LPAA", "AAA", "Test Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, "PT-LIS");

        assertNotNull(airport);
        assertEquals("LPAA", airport.icaoCode());
        assertEquals("AAA", airport.iataCode());
        assertEquals("Test Airport", airport.name());
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterAirportFailsForDuplicateIcao() {
        controller.registerAirport("LPPT", "LIS", "Airport 1", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, "PT-LIS");

        controller.registerAirport("LPPT", "OPO", "Airport 2", "Porto", "Portugal",
                41.237, -8.670, 69.0, "PT-LIS");
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterAirportFailsForDuplicateIata() {
        controller.registerAirport("LPPR", "OPO", "Airport 1", "Porto", "Portugal",
                41.237, -8.670, 69.0, "PT-LIS");

        controller.registerAirport("LPPT", "OPO", "Airport 2", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, "PT-LIS");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportFailsWhenAreaDoesNotExist() {
        controller.registerAirport("LPQQ", "LQQ", "Unknown Area Airport", "Lisboa", "Portugal",
                38.774, -9.134, 114.0, "ZZ99");
    }

    @Test
    public void ensureListAirControlAreasReturnsValues() {
        final Iterable<AirControlArea> listed = controller.listAirControlAreas();

        assertNotNull(listed);

        final List<AirControlArea> list = new ArrayList<>();
        for (final AirControlArea area : listed) {
            list.add(area);
        }

        assertFalse(list.isEmpty());
        assertTrue(list.stream().allMatch(a -> a.identity().toString() != null));
    }
}
