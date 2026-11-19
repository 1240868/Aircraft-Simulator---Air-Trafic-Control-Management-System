package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CompanyId implements ValueObject, Serializable, Comparable<CompanyId> {
    private static final long serialVersionUID = 1L;

    @Column(name = "company_id", nullable = false, updatable = false)
    private String value;

    public CompanyId(final UUID value) {
        Objects.requireNonNull(value, "CompanyId is required");
        this.value = value.toString();
    }

    protected CompanyId() {
        // for ORM
    }

    public static CompanyId newId() {
        return new CompanyId(UUID.randomUUID());
    }

    public UUID asUuid() {
        return UUID.fromString(value);
    }

    @Override
    public int compareTo(final CompanyId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (CompanyId) o;
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
