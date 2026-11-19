package eapli.aisafe.aircontrol.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AirControlAreaCode implements ValueObject, Serializable, Comparable<AirControlAreaCode> {
    private static final long serialVersionUID = 1L;

    @Column(name = "air_control_area_code", nullable = false, updatable = false, length = 20)
    private String value;

    protected AirControlAreaCode() {
    }

    public AirControlAreaCode(final String value) {
        Preconditions.nonEmpty(value, "Air control area code is required");
        final String normalized = value.trim().toUpperCase();
        Preconditions.ensure(!normalized.isEmpty(), "Air control area code is required");
        this.value = normalized;
    }

    public static AirControlAreaCode valueOf(final String raw) {
        return new AirControlAreaCode(raw);
    }

    @Override
    public int compareTo(final AirControlAreaCode o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AirControlAreaCode that = (AirControlAreaCode) o;
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
