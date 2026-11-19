package eapli.aisafe.pilot;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PilotId implements ValueObject, Serializable, Comparable<PilotId> {
    private static final long serialVersionUID = 1L;
    @Column(name = "pilot_id", nullable = false, updatable = false, length = 36)
    private String value;

    protected PilotId() {
    }

    public PilotId(final UUID value) {
        Objects.requireNonNull(value, "PilotId is required");
        this.value = value.toString();
    }

    public static PilotId newId() {
        return new PilotId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final PilotId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PilotId that = (PilotId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
