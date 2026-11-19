package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

import org.junit.Test;

public class FuelPlanTest {

    @Test
    public void shouldCreateValidFuelPlan() {
        final FuelPlan fp = new FuelPlan(5000.0, 500.0, MeasurementUnit.KILOGRAM);
        assertEquals(5000.0, fp.totalFuel(), 0.0);
        assertEquals(500.0, fp.reserveFuel(), 0.0);
        assertEquals(MeasurementUnit.KILOGRAM, fp.unit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveTotal() {
        new FuelPlan(0.0, 0.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeTotal() {
        new FuelPlan(-100.0, 0.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeReserve() {
        new FuelPlan(5000.0, -1.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReserveExceedingTotal() {
        new FuelPlan(100.0, 200.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonKilogramUnit() {
        new FuelPlan(5000.0, 500.0, MeasurementUnit.LITER);
    }
}
