package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Elevation implements ValueObject {
    private double value;
    private String unit;

    protected Elevation() {
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Elevation{" +
                "value=" + value +
                ", unit='" + unit + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Elevation elevation = (Elevation) o;
        return Double.compare(value, elevation.value) == 0 && Objects.equals(unit, elevation.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }


    public String getUnit() {
        return unit;
    }


    public Elevation(final double value, final String unit) {
        Preconditions.nonEmpty(unit, "Unit is required");
        Preconditions.nonNull(value, "Value is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
