package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class FuelSpecsTest {

    @Test
    public void validFuelSpecsShouldHoldAllFields() {
        final FuelSpecs fs = new FuelSpecs(FuelType.JET_A1, 0.8, 43e6);
        assertEquals(FuelType.JET_A1, fs.type());
        assertEquals(0.8, fs.density(), 0.001);
        assertEquals(43e6, fs.energy(), 1.0);
    }

    @Test
    public void shouldRejectNullType() {
        assertThrows(NullPointerException.class, () -> new FuelSpecs(null, 0.8, 43e6));
    }

    @Test
    public void shouldRejectZeroDensity() {
        assertThrows(Exception.class, () -> new FuelSpecs(FuelType.JET_A1, 0, 43e6));
    }

    @Test
    public void shouldRejectNegativeDensity() {
        assertThrows(Exception.class, () -> new FuelSpecs(FuelType.JET_A1, -0.1, 43e6));
    }

    @Test
    public void shouldRejectZeroEnergy() {
        assertThrows(Exception.class, () -> new FuelSpecs(FuelType.JET_A1, 0.8, 0));
    }

    @Test
    public void shouldRejectNegativeEnergy() {
        assertThrows(Exception.class, () -> new FuelSpecs(FuelType.JET_A1, 0.8, -1));
    }

    @Test
    public void electricFuelShouldBeAccepted() {
        final FuelSpecs fs = new FuelSpecs(FuelType.ELECTRICITY, 250.0, 500.0);
        assertEquals(FuelType.ELECTRICITY, fs.type());
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<FuelSpecs> ctor = FuelSpecs.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
