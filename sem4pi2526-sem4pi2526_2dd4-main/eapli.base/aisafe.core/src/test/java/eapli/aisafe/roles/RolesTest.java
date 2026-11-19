package eapli.aisafe.roles;

import eapli.framework.infrastructure.authz.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class RolesTest {

    @Test
    void adminRoleShouldBeDefined() {
        assertNotNull(Roles.ADMIN);
        assertEquals(Role.valueOf("ADMIN"), Roles.ADMIN);
    }

    @Test
    void allRolesShouldBeDefined() {
        assertNotNull(Roles.BACKOFFICE_OPERATOR);
        assertNotNull(Roles.PILOT);
        assertNotNull(Roles.FLIGHT_CONTROL_OPERATOR);
        assertNotNull(Roles.WEATHER_PERSON);
        assertNotNull(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        assertNotNull(Roles.AIR_CONTROL_AREA_COLLABORATOR);
    }

    @Test
    void constructorShouldBePrivate() throws Exception {
        Constructor<Roles> constructor = Roles.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
}