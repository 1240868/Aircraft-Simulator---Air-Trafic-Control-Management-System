package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class FuelPlan implements ValueObject {
    @Column(name = "total_fuel", nullable = false)
    private double totalFuel;

    @Column(name = "reserve_fuel", nullable = false)
    private double reserveFuel;

    @Column(name = "fuel_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementUnit unit;

    protected FuelPlan() {
    }

    public FuelPlan(final double totalFuel, final double reserveFuel, final MeasurementUnit unit) {
        Preconditions.ensure(totalFuel > 0, "Total fuel must be positive");
        Preconditions.ensure(reserveFuel >= 0, "Reserve fuel must be non-negative");
        Preconditions.ensure(reserveFuel <= totalFuel, "Reserve fuel cannot exceed total fuel");
        Preconditions.areEqual(unit, MeasurementUnit.KILOGRAM, "Fuel unit must be kilograms");
        this.totalFuel = totalFuel;
        this.reserveFuel = reserveFuel;
        this.unit = unit;
    }

    public double totalFuel() { return totalFuel; }
    public double reserveFuel() { return reserveFuel; }
    public MeasurementUnit unit() { return unit; }
}
