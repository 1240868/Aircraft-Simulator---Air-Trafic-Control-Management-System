package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email for a customer collaborator; no requirement that it matches the customer's domain.
 */
public class CollaboratorEmail implements ValueObject {
    private static final Pattern BASIC = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String value;

    protected CollaboratorEmail() {
        // ORM
    }

    public CollaboratorEmail(final String value) {
        Objects.requireNonNull(value, "Email is required");
        final var trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (!BASIC.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format");
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
        final var that = (CollaboratorEmail) o;
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
