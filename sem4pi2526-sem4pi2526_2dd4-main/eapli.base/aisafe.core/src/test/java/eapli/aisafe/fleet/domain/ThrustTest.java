package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class ThrustTest {

    @Test
    public void validThrustShouldHoldValueAndUnit() {
        final Thrust t = new Thrust(120.5, "kN");
        assertEquals(120.5, t.value(), 0.001);
        assertEquals("kN", t.unit());
    }

    @Test
    public void shouldRejectZeroValue() {
        assertThrows(Exception.class, () -> new Thrust(0, "kN"));
    }

    @Test
    public void shouldRejectNegativeValue() {
        assertThrows(Exception.class, () -> new Thrust(-1, "kN"));
    }

    @Test
    public void shouldRejectBlankUnit() {
        assertThrows(Exception.class, () -> new Thrust(100, "   "));
    }

    @Test
    public void shouldRejectNullUnit() {
        assertThrows(Exception.class, () -> new Thrust(100, null));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<Thrust> ctor = Thrust.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
