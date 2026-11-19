package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AirportId implements ValueObject, Serializable, Comparable<AirportId> {
    private static final long serialVersionUID = 1L;
    @Column(name = "airport_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected AirportId() {
    }

    public AirportId(final UUID value) {
        Objects.requireNonNull(value, "AirportId is required");
        this.value = value.toString();
    }

    public static AirportId newId() {
        return new AirportId(UUID.randomUUID());
    }

    /** References an airport by ICAO code (aggregate identity in this system). */
    public static AirportId forIcaoCode(final String icaoCode) {
        java.util.Objects.requireNonNull(icaoCode, "ICAO code is required");
        final String v = icaoCode.trim().toUpperCase();
        if (v.length() != 4) {
            throw new IllegalArgumentException("ICAO code must have 4 characters");
        }
        final AirportId id = new AirportId();
        id.value = v;
        return id;
    }

    public String code() {
        return value;
    }

    public UUID asUuid() {
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalStateException("AirportId is not a UUID reference: " + value);
        }
    }

    @Override
    public int compareTo(final AirportId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AirportId that = (AirportId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
