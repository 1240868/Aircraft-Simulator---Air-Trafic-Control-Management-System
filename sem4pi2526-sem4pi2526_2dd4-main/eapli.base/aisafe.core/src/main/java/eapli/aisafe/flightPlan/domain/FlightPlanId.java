package eapli.aisafe.flightPlan.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FlightPlanId implements ValueObject, Serializable, Comparable<FlightPlanId> {
    private static final long serialVersionUID = 1L;
    @Column(name = "flight_plan_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected FlightPlanId() {
    }

    public FlightPlanId(final UUID value) {
        Objects.requireNonNull(value, "FlightPlanId is required");
        this.value = value.toString();
    }

    public static FlightPlanId newId() {
        return new FlightPlanId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final FlightPlanId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightPlanId that = (FlightPlanId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
