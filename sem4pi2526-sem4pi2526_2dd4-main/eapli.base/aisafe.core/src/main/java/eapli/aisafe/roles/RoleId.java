package eapli.aisafe.roles;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoleId implements ValueObject, Serializable, Comparable<RoleId> {
    private static final long serialVersionUID = 1L;
    private String value;

    protected RoleId() {
    }

    public RoleId(final UUID value) {
        Objects.requireNonNull(value, "RoleId is required");
        this.value = value.toString();
    }

    public static RoleId newId() {
        return new RoleId(UUID.randomUUID());
    }

    public UUID asUuid() { return UUID.fromString(value); }

    @Override
    public int compareTo(final RoleId o) { return this.value.compareTo(o.value); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RoleId that = (RoleId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
