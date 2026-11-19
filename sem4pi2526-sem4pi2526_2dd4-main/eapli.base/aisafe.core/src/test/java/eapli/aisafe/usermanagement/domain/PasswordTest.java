package eapli.aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    private final Password passwordPolicy = new Password();

    @Test
    void isSatisfiedByShouldReturnFalseWhenPasswordIsNull() {
        assertFalse(passwordPolicy.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedByShouldReturnFalseWhenPasswordIsTooShort() {
        assertFalse(passwordPolicy.isSatisfiedBy("1234567"));
    }

    @Test
    void isSatisfiedByShouldReturnTrueWhenPasswordHasAtLeastEightCharacters() {
        assertTrue(passwordPolicy.isSatisfiedBy("12345678"));
    }
}