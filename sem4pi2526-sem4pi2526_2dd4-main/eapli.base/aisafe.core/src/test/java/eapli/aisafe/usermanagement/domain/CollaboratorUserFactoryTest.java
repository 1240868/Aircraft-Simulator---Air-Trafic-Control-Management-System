package eapli.aisafe.usermanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CollaboratorUserFactoryTest {

    @Test
    public void shouldCreateCollaboratorUserFromValidInput() {
        final var data = CollaboratorUserFactory.newCollaboratorUser(
                "collab@example.com", "Maria", "Silva", "+351912000000");

        assertNotNull(data);
        assertEquals("collab@example.com", data.email().value());
        assertEquals("Maria", data.fullName().firstName());
        assertEquals("Silva", data.fullName().lastName());
        assertEquals("+351912000000", data.phone().value());
    }

    @Test
    public void shouldPropagateInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "invalid-email", "John", "Doe", "123456"));
    }

    @Test
    public void shouldPropagateNullEmail() {
        assertThrows(NullPointerException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        null, "John", "Doe", "123456"));
    }

    @Test
    public void shouldPropagateBlankFirstName() {
        assertThrows(IllegalArgumentException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", "   ", "Doe", "123456"));
    }

    @Test
    public void shouldPropagateNullFirstName() {
        assertThrows(NullPointerException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", null, "Doe", "123456"));
    }

    @Test
    public void shouldPropagateBlankLastName() {
        assertThrows(IllegalArgumentException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", "John", "   ", "123456"));
    }

    @Test
    public void shouldPropagateNullLastName() {
        assertThrows(NullPointerException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", "John", null, "123456"));
    }

    @Test
    public void shouldPropagateInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", "John", "Doe", "abc"));
    }

    @Test
    public void shouldPropagateNullPhone() {
        assertThrows(NullPointerException.class,
                () -> CollaboratorUserFactory.newCollaboratorUser(
                        "a@b.com", "John", "Doe", null));
    }
}
