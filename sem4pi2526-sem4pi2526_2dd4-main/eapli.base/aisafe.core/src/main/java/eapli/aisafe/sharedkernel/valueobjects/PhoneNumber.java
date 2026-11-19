package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class PhoneNumber implements ValueObject {
    private String value;

    protected PhoneNumber() {
    }

    public PhoneNumber(final String value) {
        Preconditions.nonEmpty(value, "Phone number is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
