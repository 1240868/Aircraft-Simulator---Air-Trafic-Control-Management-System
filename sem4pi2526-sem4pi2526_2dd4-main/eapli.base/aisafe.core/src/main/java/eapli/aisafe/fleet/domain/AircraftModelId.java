package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AircraftModelId implements ValueObject, Serializable, Comparable<AircraftModelId> {
    private static final long serialVersionUID = 1L;

    @Column(name = "aircraft_model_id", nullable = false, updatable = false)
    private String value;

    public AircraftModelId(final UUID value) {
        Objects.requireNonNull(value, "AircraftModelId is required");
        this.value = value.toString();
    }

    protected AircraftModelId() {
        // for ORM
    }

    public static AircraftModelId newId() {
        return new AircraftModelId(UUID.randomUUID());
    }

    @Override
    public int compareTo(final AircraftModelId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
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
        final var that = (AircraftModelId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

