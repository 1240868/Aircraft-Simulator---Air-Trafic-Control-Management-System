package eapli.aisafe.flightPlan.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FlightSegmentId implements ValueObject, Serializable, Comparable<FlightSegmentId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "flight_segment_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected FlightSegmentId() {
    }

    public FlightSegmentId(final UUID value) {
        Objects.requireNonNull(value, "FlightSegmentId is required");
        this.value = value.toString();
    }

    public static FlightSegmentId newId() {
        return new FlightSegmentId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final FlightSegmentId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightSegmentId that = (FlightSegmentId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}