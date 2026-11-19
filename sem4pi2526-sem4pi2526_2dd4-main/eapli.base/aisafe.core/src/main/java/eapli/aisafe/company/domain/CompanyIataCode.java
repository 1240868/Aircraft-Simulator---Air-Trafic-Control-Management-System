package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class CompanyIataCode implements ValueObject {
    private String value;

    protected CompanyIataCode() {
        // for ORM
    }

    public CompanyIataCode(final String raw) {
        Preconditions.nonEmpty(raw, "IATA code is required");
        final String v = raw.trim().toUpperCase();
        Preconditions.ensure(v.length() == 2 && v.chars().allMatch(Character::isLetter),
                "IATA code must be exactly 2 letters");
        this.value = v;
    }

    public String value() {
        return value;
    }
}
