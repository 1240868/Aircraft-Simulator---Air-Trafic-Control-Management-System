package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class Percentage implements ValueObject {
    private double value;

    protected Percentage() {
    }

    public Percentage(final double value) {
        Preconditions.ensure(value >= 0 && value <= 100, "Percentage must be between 0 and 100");
        this.value = value;
    }

    public double value() { return value; }
}
