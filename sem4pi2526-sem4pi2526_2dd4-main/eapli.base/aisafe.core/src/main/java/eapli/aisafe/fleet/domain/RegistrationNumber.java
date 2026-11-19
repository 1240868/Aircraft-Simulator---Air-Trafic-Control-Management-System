package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Unique aircraft registration (tail number), used as aggregate identity.
 */
@Embeddable
public class RegistrationNumber implements ValueObject, Serializable, Comparable<RegistrationNumber> {
    private static final long serialVersionUID = 1L;
    private static final Pattern ALLOWED = Pattern.compile("^[A-Z0-9\\-]{2,14}$");

    @Column(name = "registration_number", nullable = false, length = 14, updatable = false)
    private String value;

    protected RegistrationNumber() {
        // for ORM
    }

    private RegistrationNumber(final String normalized) {
        this.value = normalized;
    }

    public static RegistrationNumber valueOf(final String raw) {
        Preconditions.nonEmpty(raw, "Registration number is required");
        final String normalized = raw.trim().toUpperCase().replace(" ", "");
        Preconditions.ensure(ALLOWED.matcher(normalized).matches(),
                "Registration number must be 2–14 characters (letters, digits, hyphen)");
        return new RegistrationNumber(normalized);
    }

    @Override
    public int compareTo(final RegistrationNumber o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (RegistrationNumber) o;
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
