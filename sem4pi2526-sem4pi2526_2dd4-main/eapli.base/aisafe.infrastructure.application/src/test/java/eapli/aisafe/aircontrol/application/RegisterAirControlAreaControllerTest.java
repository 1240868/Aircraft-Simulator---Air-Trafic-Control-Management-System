package eapli.aisafe.aircontrol.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class RegisterAirControlAreaControllerTest {

    private static final class FakeAuth implements AuthorizationPort {
        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
        }

        @Override
        public String currentUsername() {
            return "test-operator";
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

    private TestAirControlAreaRepository repo;
    private RegisterAirControlAreaController controller;

    @Before
    public void setUp() {
        repo = new TestAirControlAreaRepository();
        controller = new RegisterAirControlAreaController(new FakeAuth(), repo);
    }

    @Test
    public void ensureRegisterAirControlAreaReturnsArea() {
        final AirControlArea area = controller.registerAirControlArea(
                "AR01", "Area 1", -10.0, 10.0, -20.0, 20.0);

        assertNotNull(area);
        assertEquals(AirControlAreaCode.valueOf("AR01"), area.identity());
        assertEquals("Area 1", area.name());
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterAirControlAreaFailsForDuplicateCode() {
        controller.registerAirControlArea("AR02", "Area 1", -10.0, 10.0, -20.0, 20.0);
        controller.registerAirControlArea("AR02", "Area 2", -5.0, 5.0, -10.0, 10.0);
    }
}
