package eapli.aisafe.aircraftmaker;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class OriginCountry implements ValueObject {
    private String value;

    protected OriginCountry() {
    }

    public OriginCountry(final String value) {
        Preconditions.nonEmpty(value, "Origin country is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
