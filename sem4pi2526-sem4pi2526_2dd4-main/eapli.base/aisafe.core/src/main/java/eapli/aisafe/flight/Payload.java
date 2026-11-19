package eapli.aisafe.flight;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class Payload implements ValueObject {
    private double passengers;
    private double cargo;
    private double baggage;
    private String unit;

    protected Payload() {
    }

    public Payload(final double passengers, final double cargo, final double baggage, final String unit) {
        Preconditions.ensure(passengers >= 0, "Passengers must be non-negative");
        Preconditions.ensure(cargo >= 0, "Cargo must be non-negative");
        Preconditions.ensure(baggage >= 0, "Baggage must be non-negative");
        Preconditions.nonEmpty(unit, "Unit is required");
        this.passengers = passengers;
        this.cargo = cargo;
        this.baggage = baggage;
        this.unit = unit;
    }

    public double passengers() { return passengers; }
    public double cargo() { return cargo; }
    public double baggage() { return baggage; }
    public String unit() { return unit; }
}
