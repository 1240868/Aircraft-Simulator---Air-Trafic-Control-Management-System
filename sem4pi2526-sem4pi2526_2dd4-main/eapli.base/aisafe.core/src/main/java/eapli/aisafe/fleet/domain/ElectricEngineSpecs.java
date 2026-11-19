package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;

@Embeddable
public class ElectricEngineSpecs implements ValueObject {

    @Embedded
    private Power power;

    @Column(name = "electric_efficiency", nullable = true)
    private double efficiency;

    protected ElectricEngineSpecs() {
        // for ORM
    }

    public ElectricEngineSpecs(final Power power, final double efficiency) {
        this.power = java.util.Objects.requireNonNull(power, "Power is required");
        Preconditions.ensure(efficiency > 0, "Efficiency must be positive");
        this.efficiency = efficiency;
    }

    public Power power() {
        return power;
    }

    public double efficiency() {
        return efficiency;
    }
}

