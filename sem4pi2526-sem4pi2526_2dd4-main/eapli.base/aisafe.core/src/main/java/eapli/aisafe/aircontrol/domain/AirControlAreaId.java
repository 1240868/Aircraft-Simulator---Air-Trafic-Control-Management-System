package eapli.aisafe.aircontrol.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AirControlAreaId implements ValueObject, Serializable, Comparable<AirControlAreaId> {
    private static final long serialVersionUID = 1L;
    private String value;

    protected AirControlAreaId() {
    }

    public AirControlAreaId(final UUID value) {
        Objects.requireNonNull(value, "AirControlAreaId is required");
        this.value = value.toString();
    }

    public static AirControlAreaId newId() {
        return new AirControlAreaId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final AirControlAreaId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AirControlAreaId that = (AirControlAreaId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
