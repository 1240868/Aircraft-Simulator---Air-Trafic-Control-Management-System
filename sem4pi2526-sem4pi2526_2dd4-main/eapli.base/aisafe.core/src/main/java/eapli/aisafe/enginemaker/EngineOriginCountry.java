package eapli.aisafe.enginemaker;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class EngineOriginCountry implements ValueObject {
    private String value;

    protected EngineOriginCountry() {
    }

    public EngineOriginCountry(final String value) {
        Preconditions.nonEmpty(value, "Engine origin country is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
