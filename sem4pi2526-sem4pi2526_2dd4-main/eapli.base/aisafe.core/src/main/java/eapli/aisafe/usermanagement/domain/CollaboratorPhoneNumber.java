package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import java.util.Objects;

/**
 * Phone number for a collaborator; digits with optional leading + and separators.
 */
public class CollaboratorPhoneNumber implements ValueObject {
    private static final int MIN_DIGITS = 3;
    private static final int MAX_LENGTH = 32;

    private String value;

    protected CollaboratorPhoneNumber() {
        // ORM
    }

    public CollaboratorPhoneNumber(final String value) {
        Objects.requireNonNull(value, "Phone is required");
        final var trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Phone must not be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Phone is too long");
        }
        final var digits = trimmed.chars()
                .filter(Character::isDigit)
                .count();
        if (digits < MIN_DIGITS) {
            throw new IllegalArgumentException("Phone must contain at least " + MIN_DIGITS + " digits");
        }
        if (!trimmed.matches("[+0-9\\s().-]+")) {
            throw new IllegalArgumentException("Phone contains invalid characters");
        }
        this.value = trimmed;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (CollaboratorPhoneNumber) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
