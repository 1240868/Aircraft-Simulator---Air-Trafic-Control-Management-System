package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class FuelQuantity implements ValueObject {
    private double value;
    private String unit;

    protected FuelQuantity() {
    }

    public FuelQuantity(final double value, final String unit) {
        Preconditions.ensure(value >= 0, "Fuel quantity must be non-negative");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.value = value;
        this.unit = unit;
    }

    public double value() { return value; }
    public String unit() { return unit; }
}
