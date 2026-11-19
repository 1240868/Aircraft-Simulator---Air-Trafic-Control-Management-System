package eapli.aisafe.usermanagement.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class CollaboratorPhoneNumberTest {

    @Test
    public void shouldCreateValidPhoneNumber() {
        final var phone = new CollaboratorPhoneNumber("+351 912 345 678");
        assertEquals("+351 912 345 678", phone.value());
    }

    @Test
    public void shouldTrimWhitespace() {
        final var phone = new CollaboratorPhoneNumber("  123456  ");
        assertEquals("123456", phone.value());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNull() {
        new CollaboratorPhoneNumber(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmpty() {
        new CollaboratorPhoneNumber("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlank() {
        new CollaboratorPhoneNumber("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTooLong() {
        new CollaboratorPhoneNumber("1".repeat(33));
    }

    @Test
    public void shouldAcceptMaxLength() {
        final var maxPhone = "1".repeat(32);
        final var phone = new CollaboratorPhoneNumber(maxPhone);
        assertEquals(maxPhone, phone.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTooFewDigits() {
        new CollaboratorPhoneNumber("12");
    }

    @Test
    public void shouldAcceptExactlyMinDigits() {
        final var phone = new CollaboratorPhoneNumber("123");
        assertEquals("123", phone.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidCharacters() {
        new CollaboratorPhoneNumber("123abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectLettersOnly() {
        new CollaboratorPhoneNumber("abcdefgh");
    }

    @Test
    public void shouldAcceptPhoneWithParentheses() {
        final var phone = new CollaboratorPhoneNumber("(+351) 912-345-678");
        assertEquals("(+351) 912-345-678", phone.value());
    }

    @Test
    public void shouldAcceptPhoneWithDots() {
        final var phone = new CollaboratorPhoneNumber("912.345.678");
        assertEquals("912.345.678", phone.value());
    }

    @Test
    public void shouldAcceptPhoneWithDashes() {
        final var phone = new CollaboratorPhoneNumber("912-345-678");
        assertEquals("912-345-678", phone.value());
    }

    @Test
    public void shouldAcceptPhoneWithSpaces() {
        final var phone = new CollaboratorPhoneNumber("912 345 678");
        assertEquals("912 345 678", phone.value());
    }

    @Test
    public void equalsSameValue() {
        final var a = new CollaboratorPhoneNumber("+351912345678");
        final var b = new CollaboratorPhoneNumber("+351912345678");
        assertTrue(a.equals(b));
    }

    @Test
    public void equalsDifferentValue() {
        final var a = new CollaboratorPhoneNumber("123456789");
        final var b = new CollaboratorPhoneNumber("987654321");
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsSelf() {
        final var phone = new CollaboratorPhoneNumber("123456789");
        assertTrue(phone.equals(phone));
    }

    @Test
    public void equalsNull() {
        final var phone = new CollaboratorPhoneNumber("123456789");
        assertFalse(phone.equals(null));
    }

    @Test
    public void equalsDifferentType() {
        final var phone = new CollaboratorPhoneNumber("123456789");
        assertFalse(phone.equals("123456789"));
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        final var a = new CollaboratorPhoneNumber("+351912345678");
        final var b = new CollaboratorPhoneNumber("+351912345678");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringShouldReturnValue() {
        final var phone = new CollaboratorPhoneNumber("123456789");
        assertEquals("123456789", phone.toString());
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final Constructor<CollaboratorPhoneNumber> ctor =
                CollaboratorPhoneNumber.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var phone = ctor.newInstance();
        assertNotNull(phone);
    }
}
