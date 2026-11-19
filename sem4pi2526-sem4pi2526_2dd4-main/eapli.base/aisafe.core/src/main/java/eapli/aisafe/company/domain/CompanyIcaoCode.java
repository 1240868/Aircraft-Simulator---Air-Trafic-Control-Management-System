package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class CompanyIcaoCode implements ValueObject {
    private String value;

    protected CompanyIcaoCode() {
        // for ORM
    }

    /**
     * Airline ICAO designator: 2–3 letters (per project brief).
     */
    public CompanyIcaoCode(final String raw) {
        Preconditions.nonEmpty(raw, "ICAO code is required");
        final String v = raw.trim().toUpperCase();
        Preconditions.ensure(v.length() >= 2 && v.length() <= 3 && v.chars().allMatch(Character::isLetter),
                "ICAO code must be 2 or 3 letters");
        this.value = v;
    }

    public String value() {
        return value;
    }
}
