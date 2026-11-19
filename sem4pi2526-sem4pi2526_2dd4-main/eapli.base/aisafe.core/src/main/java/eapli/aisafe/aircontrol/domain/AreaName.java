package eapli.aisafe.aircontrol.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class AreaName implements ValueObject {
    private String value;

    protected AreaName() {
    }

    public AreaName(final String value) {
        Preconditions.nonEmpty(value, "Area name is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
