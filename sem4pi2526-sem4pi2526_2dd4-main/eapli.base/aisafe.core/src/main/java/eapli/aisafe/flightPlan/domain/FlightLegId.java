package eapli.aisafe.flightPlan.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FlightLegId implements ValueObject, Serializable, Comparable<FlightLegId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "flight_leg_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected FlightLegId() {
    }

    public FlightLegId(final UUID value) {
        Objects.requireNonNull(value, "FlightLegId is required");
        this.value = value.toString();
    }

    public static FlightLegId newId() {
        return new FlightLegId(UUID.randomUUID());
    }

    public UUID asUuid() {
        return UUID.fromString(value);
    }

    @Override
    public int compareTo(final FlightLegId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightLegId that = (FlightLegId) o;
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
