package eapli.aisafe.usermanagement.domain;

import static org.junit.Assert.*;

import org.junit.Test;

public class CollaboratorUserDataTest {

    private static CollaboratorEmail email(final String v) {
        return new CollaboratorEmail(v);
    }

    private static CollaboratorFullName name(final String f, final String l) {
        return new CollaboratorFullName(f, l);
    }

    private static CollaboratorPhoneNumber phone(final String v) {
        return new CollaboratorPhoneNumber(v);
    }

    @Test
    public void shouldCreateValidUserData() {
        final var data = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertNotNull(data);
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullEmail() {
        new CollaboratorUserData(null, name("J", "D"), phone("123456"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullFullName() {
        new CollaboratorUserData(email("a@b.com"), null, phone("123456"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullPhone() {
        new CollaboratorUserData(email("a@b.com"), name("J", "D"), null);
    }

    @Test
    public void emailGetter() {
        final var e = email("a@b.com");
        final var data = new CollaboratorUserData(e, name("J", "D"), phone("123456"));
        assertEquals(e, data.email());
    }

    @Test
    public void fullNameGetter() {
        final var n = name("John", "Doe");
        final var data = new CollaboratorUserData(email("a@b.com"), n, phone("123456"));
        assertEquals(n, data.fullName());
    }

    @Test
    public void phoneGetter() {
        final var p = phone("123456");
        final var data = new CollaboratorUserData(email("a@b.com"), name("J", "D"), p);
        assertEquals(p, data.phone());
    }

    @Test
    public void equalsSameValues() {
        final var a = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        final var b = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertTrue(a.equals(b));
    }

    @Test
    public void equalsSelf() {
        final var data = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertTrue(data.equals(data));
    }

    @Test
    public void equalsNull() {
        final var data = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertFalse(data.equals(null));
    }

    @Test
    public void equalsDifferentType() {
        final var data = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertFalse(data.equals("some string"));
    }

    @Test
    public void equalsDifferentEmail() {
        final var a = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        final var b = new CollaboratorUserData(
                email("x@y.com"), name("John", "Doe"), phone("123456"));
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferentName() {
        final var a = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        final var b = new CollaboratorUserData(
                email("a@b.com"), name("Jane", "Doe"), phone("123456"));
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferentPhone() {
        final var a = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        final var b = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("654321"));
        assertFalse(a.equals(b));
    }

    @Test
    public void hashCodeConsistentWithEquals() {
        final var a = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        final var b = new CollaboratorUserData(
                email("a@b.com"), name("John", "Doe"), phone("123456"));
        assertEquals(a.hashCode(), b.hashCode());
    }
}
