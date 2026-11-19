package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class PowerTest {

    @Test
    public void validPowerShouldHoldValueAndUnit() {
        final Power p = new Power(2.5, "MW");
        assertEquals(2.5, p.value(), 0.001);
        assertEquals("MW", p.unit());
    }

    @Test
    public void shouldRejectZeroValue() {
        assertThrows(Exception.class, () -> new Power(0, "MW"));
    }

    @Test
    public void shouldRejectNegativeValue() {
        assertThrows(Exception.class, () -> new Power(-1, "MW"));
    }

    @Test
    public void shouldRejectBlankUnit() {
        assertThrows(Exception.class, () -> new Power(1, "   "));
    }

    @Test
    public void shouldRejectNullUnit() {
        assertThrows(Exception.class, () -> new Power(1, null));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<Power> ctor = Power.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
