package eapli.aisafe.flightroute;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FlightRouteId implements ValueObject, Serializable, Comparable<FlightRouteId> {
    private static final long serialVersionUID = 1L;
    @Column(name = "flight_route_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected FlightRouteId() {
    }

    public FlightRouteId(final UUID value) {
        Objects.requireNonNull(value, "FlightRouteId is required");
        this.value = value.toString();
    }

    public static FlightRouteId newId() {
        return new FlightRouteId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final FlightRouteId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightRouteId that = (FlightRouteId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
