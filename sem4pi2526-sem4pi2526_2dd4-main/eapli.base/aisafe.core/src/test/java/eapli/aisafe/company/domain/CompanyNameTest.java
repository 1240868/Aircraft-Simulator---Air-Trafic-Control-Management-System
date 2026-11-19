package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class CompanyNameTest {

    @Test
    public void shouldCreateValidName() {
        final var name = new CompanyName("TAP Air Portugal");
        assertNotNull(name);
    }

    @Test
    public void shouldTrimWhitespace() {
        final var name = new CompanyName("  TAP  ");
        assertEquals("TAP", name.value());
    }

    @Test
    public void valueShouldReturnName() {
        final var name = new CompanyName("TAP Air Portugal");
        assertEquals("TAP Air Portugal", name.value());
    }

    @Test
    public void shouldRejectNull() {
        assertThrows(Exception.class, () -> new CompanyName(null));
    }

    @Test
    public void shouldRejectEmpty() {
        assertThrows(Exception.class, () -> new CompanyName(""));
    }

    @Test
    public void shouldRejectBlank() {
        assertThrows(Exception.class, () -> new CompanyName("   "));
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final Constructor<CompanyName> ctor =
                CompanyName.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var name = ctor.newInstance();
        assertNotNull(name);
    }
}
