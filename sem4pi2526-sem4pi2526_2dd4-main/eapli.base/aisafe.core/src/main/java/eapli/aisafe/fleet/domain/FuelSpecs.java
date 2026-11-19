package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class FuelSpecs implements ValueObject {

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType type;

    @Column(name = "fuel_density", nullable = false)
    private double density;

    @Column(name = "fuel_specific_energy", nullable = false)
    private double energy;

    protected FuelSpecs() {
        // for ORM
    }

    public FuelSpecs(final FuelType type, final double density, final double energy) {
        this.type = java.util.Objects.requireNonNull(type, "Fuel type is required");
        Preconditions.ensure(density > 0, "Fuel density must be positive");
        Preconditions.ensure(energy > 0, "Fuel energy must be positive");
        this.density = density;
        this.energy = energy;
    }

    public FuelType type() {
        return type;
    }

    public double density() {
        return density;
    }

    public double energy() {
        return energy;
    }
}

