package eapli.aisafe.aircraftmaker;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MakerId implements ValueObject, Serializable, Comparable<MakerId> {
    private static final long serialVersionUID = 1L;
    private String value;

    protected MakerId() {
    }

    public MakerId(final UUID value) {
        Objects.requireNonNull(value, "MakerId is required");
        this.value = value.toString();
    }

    public static MakerId newId() {
        return new MakerId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final MakerId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MakerId that = (MakerId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
