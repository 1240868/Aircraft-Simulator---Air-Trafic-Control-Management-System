package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Country where the aircraft is registered (ISO 3166-1 alpha-2).
 */
@Embeddable
public class AircraftRegistrationCountry implements ValueObject {
    private static final Pattern ISO_ALPHA2 = Pattern.compile("^[A-Z]{2}$");

    @Column(name = "registration_country_code", nullable = false, length = 2)
    private String value;

    protected AircraftRegistrationCountry() {
        // for ORM
    }

    private AircraftRegistrationCountry(final String value) {
        this.value = value;
    }

    public static AircraftRegistrationCountry valueOf(final String raw) {
        Preconditions.nonEmpty(raw, "Registration country is required");
        final String normalized = raw.trim().toUpperCase();
        Preconditions.ensure(ISO_ALPHA2.matcher(normalized).matches(),
                "Registration country must be a 2-letter ISO 3166-1 alpha-2 code");
        return new AircraftRegistrationCountry(normalized);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (AircraftRegistrationCountry) o;
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
