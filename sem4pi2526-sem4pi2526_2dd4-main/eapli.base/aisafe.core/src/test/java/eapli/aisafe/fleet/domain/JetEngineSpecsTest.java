package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class JetEngineSpecsTest {

    private static Thrust seaLevel() {
        return new Thrust(120, "kN");
    }

    private static Thrust cruise() {
        return new Thrust(80, "kN");
    }

    @Test
    public void validSpecsShouldHoldAllFields() {
        final JetEngineSpecs s = new JetEngineSpecs(seaLevel(), cruise(), 0.05);
        assertEquals(120.0, s.atSeaLevelStatic().value(), 0.001);
        assertEquals(80.0, s.atCruiseSpeed().value(), 0.001);
        assertEquals(0.05, s.tsfc(), 0.0001);
    }

    @Test
    public void shouldRejectNullSeaLevelThrust() {
        assertThrows(NullPointerException.class, () -> new JetEngineSpecs(null, cruise(), 0.05));
    }

    @Test
    public void shouldRejectNullCruiseThrust() {
        assertThrows(NullPointerException.class, () -> new JetEngineSpecs(seaLevel(), null, 0.05));
    }

    @Test
    public void shouldRejectZeroTsfc() {
        assertThrows(Exception.class, () -> new JetEngineSpecs(seaLevel(), cruise(), 0));
    }

    @Test
    public void shouldRejectNegativeTsfc() {
        assertThrows(Exception.class, () -> new JetEngineSpecs(seaLevel(), cruise(), -0.01));
    }

    @Test
    public void shouldRejectCruiseThrustExceedingSeaLevel() {
        assertThrows(Exception.class, () -> new JetEngineSpecs(
                new Thrust(50, "kN"),
                new Thrust(80, "kN"),
                0.04));
    }

    @Test
    public void shouldAcceptCruiseThrustEqualToSeaLevel() {
        final JetEngineSpecs s = new JetEngineSpecs(
                new Thrust(100, "kN"),
                new Thrust(100, "kN"),
                0.04);
        assertEquals(100.0, s.atCruiseSpeed().value(), 0.001);
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<JetEngineSpecs> ctor = JetEngineSpecs.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
