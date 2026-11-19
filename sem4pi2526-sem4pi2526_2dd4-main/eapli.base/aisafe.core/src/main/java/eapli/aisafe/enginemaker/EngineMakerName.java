package eapli.aisafe.enginemaker;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class EngineMakerName implements ValueObject {
    private String value;

    protected EngineMakerName() {
    }

    public EngineMakerName(final String value) {
        Preconditions.nonEmpty(value, "Engine maker name is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
