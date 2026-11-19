package eapli.aisafe.flight;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class FlightDesignator implements ValueObject {
    private String value;

    protected FlightDesignator() {
    }

    public FlightDesignator(final String value) {
        Preconditions.nonEmpty(value, "Flight designator is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
