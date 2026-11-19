package eapli.aisafe.usermanagement.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class CollaboratorFullNameTest {

    @Test
    public void shouldCreateValidFullName() {
        final var name = new CollaboratorFullName("John", "Doe");
        assertEquals("John", name.firstName());
        assertEquals("Doe", name.lastName());
    }

    @Test
    public void shouldTrimWhitespace() {
        final var name = new CollaboratorFullName("  John  ", "  Doe  ");
        assertEquals("John", name.firstName());
        assertEquals("Doe", name.lastName());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullFirstName() {
        new CollaboratorFullName(null, "Doe");
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullLastName() {
        new CollaboratorFullName("John", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankFirstName() {
        new CollaboratorFullName("   ", "Doe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankLastName() {
        new CollaboratorFullName("John", "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyFirstName() {
        new CollaboratorFullName("", "Doe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyLastName() {
        new CollaboratorFullName("John", "");
    }

    @Test
    public void firstNameGetter() {
        assertEquals("Alice", new CollaboratorFullName("Alice", "Smith").firstName());
    }

    @Test
    public void lastNameGetter() {
        assertEquals("Smith", new CollaboratorFullName("Alice", "Smith").lastName());
    }

    @Test
    public void equalsSameValue() {
        final var a = new CollaboratorFullName("John", "Doe");
        final var b = new CollaboratorFullName("John", "Doe");
        assertTrue(a.equals(b));
    }

    @Test
    public void equalsDifferentFirstName() {
        final var a = new CollaboratorFullName("John", "Doe");
        final var b = new CollaboratorFullName("Jane", "Doe");
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferentLastName() {
        final var a = new CollaboratorFullName("John", "Doe");
        final var b = new CollaboratorFullName("John", "Smith");
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsSelf() {
        final var name = new CollaboratorFullName("John", "Doe");
        assertTrue(name.equals(name));
    }

    @Test
    public void equalsNull() {
        final var name = new CollaboratorFullName("John", "Doe");
        assertFalse(name.equals(null));
    }

    @Test
    public void equalsDifferentType() {
        final var name = new CollaboratorFullName("John", "Doe");
        assertFalse(name.equals("John Doe"));
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        final var a = new CollaboratorFullName("John", "Doe");
        final var b = new CollaboratorFullName("John", "Doe");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringShouldReturnFullName() {
        final var name = new CollaboratorFullName("John", "Doe");
        assertEquals("John Doe", name.toString());
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final Constructor<CollaboratorFullName> ctor =
                CollaboratorFullName.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var name = ctor.newInstance();
        assertNotNull(name);
    }
}
