package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class RegistrationNumberTest {

    @Test
    public void shouldCreateFromValidString() {
        assertEquals("CS-TTK", RegistrationNumber.valueOf("CS-TTK").toString());
    }

    @Test
    public void shouldNormalizeToUpperCase() {
        assertEquals("CS-TTK", RegistrationNumber.valueOf("cs-ttk").toString());
    }

    @Test
    public void shouldTrimWhitespace() {
        assertEquals("CS-TTK", RegistrationNumber.valueOf("  CS-TTK  ").toString());
    }

    @Test
    public void shouldRemoveSpaces() {
        assertEquals("CSTTK", RegistrationNumber.valueOf("CS TTK").toString());
    }

    @Test
    public void shouldAcceptMinLength() {
        assertEquals("AB", RegistrationNumber.valueOf("AB").toString());
    }

    @Test
    public void shouldAcceptMaxLength() {
        assertEquals("ABCDE12345FGHI", RegistrationNumber.valueOf("ABCDE12345FGHI").toString());
    }

    @Test
    public void shouldAcceptDigitsAndHyphens() {
        assertEquals("N-12345", RegistrationNumber.valueOf("N-12345").toString());
    }

    @Test
    public void shouldRejectNull() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf(null));
    }

    @Test
    public void shouldRejectEmpty() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf(""));
    }

    @Test
    public void shouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf("   "));
    }

    @Test
    public void shouldRejectSingleChar() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf("A"));
    }

    @Test
    public void shouldRejectTooLong() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf("ABCDE12345FGHIJ"));
    }

    @Test
    public void shouldRejectInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationNumber.valueOf("CS@TTK"));
    }

    @Test
    public void equalsSameValue() {
        assertEquals(RegistrationNumber.valueOf("CS-TTK"), RegistrationNumber.valueOf("cs-ttk"));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(RegistrationNumber.valueOf("CS-TTK"), RegistrationNumber.valueOf("CS-TTX"));
    }

    @Test
    public void equalsSameInstance() {
        final var a = RegistrationNumber.valueOf("CS-TTK");
        assertEquals(a, a);
    }

    @Test
    public void equalsNull() {
        assertFalse(RegistrationNumber.valueOf("CS-TTK").equals(null));
    }

    @Test
    public void equalsDifferentType() {
        assertFalse(RegistrationNumber.valueOf("CS-TTK").equals("CS-TTK"));
    }

    @Test
    public void hashCodeEqual() {
        assertEquals(RegistrationNumber.valueOf("CS-TTK").hashCode(), RegistrationNumber.valueOf("cs-ttk").hashCode());
    }

    @Test
    public void hashCodeDifferent() {
        assertNotEquals(RegistrationNumber.valueOf("AA-AAA").hashCode(), RegistrationNumber.valueOf("ZZ-ZZZ").hashCode());
    }

    @Test
    public void compareToLess() {
        assertTrue(RegistrationNumber.valueOf("AA-AAA").compareTo(RegistrationNumber.valueOf("ZZ-ZZZ")) < 0);
    }

    @Test
    public void compareToGreater() {
        assertTrue(RegistrationNumber.valueOf("ZZ-ZZZ").compareTo(RegistrationNumber.valueOf("AA-AAA")) > 0);
    }

    @Test
    public void compareToEqual() {
        assertEquals(0, RegistrationNumber.valueOf("CS-TTK").compareTo(RegistrationNumber.valueOf("CS-TTK")));
    }

    @Test
    public void protectedConstructorShouldExist() throws Exception {
        final Constructor<RegistrationNumber> ctor = RegistrationNumber.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
