package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class ElectricEngineSpecsTest {

    private static Power validPower() {
        return new Power(2.0, "MW");
    }

    @Test
    public void validSpecsShouldHoldPowerAndEfficiency() {
        final ElectricEngineSpecs s = new ElectricEngineSpecs(validPower(), 0.9);
        assertEquals(2.0, s.power().value(), 0.001);
        assertEquals(0.9, s.efficiency(), 0.001);
    }

    @Test
    public void shouldRejectNullPower() {
        assertThrows(NullPointerException.class, () -> new ElectricEngineSpecs(null, 0.9));
    }

    @Test
    public void shouldRejectZeroEfficiency() {
        assertThrows(Exception.class, () -> new ElectricEngineSpecs(validPower(), 0));
    }

    @Test
    public void shouldRejectNegativeEfficiency() {
        assertThrows(Exception.class, () -> new ElectricEngineSpecs(validPower(), -0.5));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<ElectricEngineSpecs> ctor = ElectricEngineSpecs.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
