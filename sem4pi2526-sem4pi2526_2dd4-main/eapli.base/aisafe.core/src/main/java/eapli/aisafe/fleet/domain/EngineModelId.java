package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class EngineModelId implements ValueObject, Serializable, Comparable<EngineModelId> {
    private static final long serialVersionUID = 1L;

    @Column(name = "engine_model_id", nullable = false, updatable = false)
    private String value;

    public EngineModelId(final UUID value) {
        Objects.requireNonNull(value, "EngineModelId is required");
        this.value = value.toString();
    }

    protected EngineModelId() {
        // for ORM
    }

    public static EngineModelId newId() {
        return new EngineModelId(UUID.randomUUID());
    }

    public UUID asUuid() {
        return UUID.fromString(value);
    }

    @Override
    public int compareTo(final EngineModelId o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value;
    }
}

