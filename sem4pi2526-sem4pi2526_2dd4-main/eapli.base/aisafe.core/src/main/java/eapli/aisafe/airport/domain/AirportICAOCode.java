package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class AirportICAOCode implements ValueObject {
    private String value;

    protected AirportICAOCode() {
    }

    public AirportICAOCode(final String value) {
        Preconditions.nonEmpty(value, "ICAO code is required");
        Preconditions.ensure(value.length() == 4, "ICAO code must be 4 characters");
        this.value = value.toUpperCase().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AirportICAOCode that = (AirportICAOCode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "AirportICAOCode{" +
                "value='" + value + '\'' +
                '}';
    }

    public String getValue() {
        return value;
    }


    public String value() { return value; }
}
