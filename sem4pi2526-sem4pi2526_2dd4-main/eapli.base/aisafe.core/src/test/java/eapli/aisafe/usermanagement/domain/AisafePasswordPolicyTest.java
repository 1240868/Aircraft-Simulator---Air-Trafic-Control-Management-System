package eapli.aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AisafePasswordPolicyTest {

    private final AisafePasswordPolicy policy = new AisafePasswordPolicy();

    @Test
    void isSatisfiedByShouldReturnFalseWhenPasswordIsNull() {
        assertFalse(policy.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedByShouldReturnFalseWhenPasswordIsTooShort() {
        assertFalse(policy.isSatisfiedBy("12345"));
    }

    @Test
    void isSatisfiedByShouldReturnTrueWhenPasswordHasAtLeastSixCharacters() {
        assertTrue(policy.isSatisfiedBy("123456"));
    }
}