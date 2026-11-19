package eapli.aisafe.aircraftmaker;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class MakerName implements ValueObject {
    private String value;

    protected MakerName() {
    }

    public MakerName(final String value) {
        Preconditions.nonEmpty(value, "Maker name is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
