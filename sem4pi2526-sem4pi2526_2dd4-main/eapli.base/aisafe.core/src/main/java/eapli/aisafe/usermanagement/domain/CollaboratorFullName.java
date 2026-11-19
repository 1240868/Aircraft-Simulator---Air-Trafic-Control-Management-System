package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import java.util.Objects;

public class CollaboratorFullName implements ValueObject {

    private String firstName;
    private String lastName;

    protected CollaboratorFullName() {
        // ORM
    }

    public CollaboratorFullName(final String firstName, final String lastName) {
        this.firstName = requireNonBlank(firstName, "First name");
        this.lastName = requireNonBlank(lastName, "Last name");
    }

    private static String requireNonBlank(final String s, final String label) {
        Objects.requireNonNull(s, label + " is required");
        final var t = s.strip();
        if (t.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return t;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (CollaboratorFullName) o;
        return firstName.equals(that.firstName) && lastName.equals(that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
