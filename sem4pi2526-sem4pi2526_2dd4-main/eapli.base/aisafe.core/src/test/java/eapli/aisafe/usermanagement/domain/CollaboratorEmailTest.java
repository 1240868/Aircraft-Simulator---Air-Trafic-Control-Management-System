package eapli.aisafe.usermanagement.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class CollaboratorEmailTest {

    @Test
    public void shouldCreateValidEmail() {
        final var email = new CollaboratorEmail("user@example.com");
        assertEquals("user@example.com", email.value());
    }

    @Test
    public void shouldTrimWhitespace() {
        final var email = new CollaboratorEmail("  user@example.com  ");
        assertEquals("user@example.com", email.value());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNull() {
        new CollaboratorEmail(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmpty() {
        new CollaboratorEmail("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyAfterTrim() {
        new CollaboratorEmail("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankTab() {
        new CollaboratorEmail("\t");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingAtSign() {
        new CollaboratorEmail("userexample.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingDomain() {
        new CollaboratorEmail("user@");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingLocalPart() {
        new CollaboratorEmail("@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingTld() {
        new CollaboratorEmail("user@example");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSpacesInEmail() {
        new CollaboratorEmail("user @example.com");
    }

    @Test
    public void shouldAcceptSubdomainEmail() {
        final var email = new CollaboratorEmail("user@sub.example.com");
        assertEquals("user@sub.example.com", email.value());
    }

    @Test
    public void shouldAcceptEmailWithPlusSign() {
        final var email = new CollaboratorEmail("user+tag@example.com");
        assertEquals("user+tag@example.com", email.value());
    }

    @Test
    public void equalsSameValue() {
        final var a = new CollaboratorEmail("test@example.com");
        final var b = new CollaboratorEmail("test@example.com");
        assertTrue(a.equals(b));
    }

    @Test
    public void equalsDifferentValue() {
        final var a = new CollaboratorEmail("a@example.com");
        final var b = new CollaboratorEmail("b@example.com");
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsSelf() {
        final var email = new CollaboratorEmail("test@example.com");
        assertTrue(email.equals(email));
    }

    @Test
    public void equalsNull() {
        final var email = new CollaboratorEmail("test@example.com");
        assertFalse(email.equals(null));
    }

    @Test
    public void equalsDifferentType() {
        final var email = new CollaboratorEmail("test@example.com");
        assertFalse(email.equals("test@example.com"));
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        final var a = new CollaboratorEmail("test@example.com");
        final var b = new CollaboratorEmail("test@example.com");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringShouldReturnValue() {
        final var email = new CollaboratorEmail("test@example.com");
        assertEquals("test@example.com", email.toString());
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final Constructor<CollaboratorEmail> ctor =
                CollaboratorEmail.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var email = ctor.newInstance();
        assertNotNull(email);
    }
}
