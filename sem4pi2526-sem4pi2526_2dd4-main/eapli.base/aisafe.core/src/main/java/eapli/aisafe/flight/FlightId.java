package eapli.aisafe.flight;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FlightId implements ValueObject, Serializable, Comparable<FlightId> {
    private static final long serialVersionUID = 1L;
    private String value;

    protected FlightId() {
    }

    public FlightId(final UUID value) {
        Objects.requireNonNull(value, "FlightId is required");
        this.value = value.toString();
    }

    public static FlightId newId() {
        return new FlightId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final FlightId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightId that = (FlightId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
