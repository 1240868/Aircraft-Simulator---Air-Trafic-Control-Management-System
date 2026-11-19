package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisafeUserBuilderHelperTest {

    @Test
    void builderShouldReturnNonNullSystemUserBuilder() {
        SystemUserBuilder builder = AisafeUserBuilderHelper.builder();

        assertNotNull(builder);
        assertInstanceOf(SystemUserBuilder.class, builder);
    }
}