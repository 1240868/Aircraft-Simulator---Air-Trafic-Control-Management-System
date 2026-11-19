package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class CompanyName implements ValueObject {
    private String value;

    protected CompanyName() {
        // for ORM
    }

    public CompanyName(final String value) {
        Preconditions.nonEmpty(value, "Company name is required");
        this.value = value.trim();
    }

    public String value() {
        return value;
    }
}
