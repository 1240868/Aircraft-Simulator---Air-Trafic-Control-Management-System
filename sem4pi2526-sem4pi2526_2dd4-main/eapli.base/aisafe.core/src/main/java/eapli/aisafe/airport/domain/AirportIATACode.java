package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class AirportIATACode implements ValueObject {
    private String value;

    protected AirportIATACode() {
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "AirportIATACode{" +
                "value='" + value + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AirportIATACode that = (AirportIATACode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    public AirportIATACode(final String value) {
        Preconditions.nonEmpty(value, "IATA code is required");
        Preconditions.ensure(value.length() == 3, "IATA code must be 3 characters");
        this.value = value.toUpperCase().trim();
    }

    public String value() { return value; }
}
