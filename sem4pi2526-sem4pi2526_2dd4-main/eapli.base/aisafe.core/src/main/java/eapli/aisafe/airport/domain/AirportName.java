package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class AirportName implements ValueObject {
    private String value;

    protected AirportName() {
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AirportName that = (AirportName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }


    @Override
    public String toString() {
        return "AirportName{" +
                "value='" + value + '\'' +
                '}';
    }

    public AirportName(final String value) {
        Preconditions.nonEmpty(value, "Airport name is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
