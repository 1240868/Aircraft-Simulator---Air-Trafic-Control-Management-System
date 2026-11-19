package eapli.aisafe.company.domain;

import eapli.aisafe.usermanagement.domain.CollaboratorPhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Link between a customer (company or air control area) and a system user, including contact phone.
 */
@Embeddable
public class CustomerCollaboratorRef implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "collaborator_username", nullable = false, length = 120)
    private String username;

    @Column(name = "collaborator_phone", nullable = false, length = 32)
    private String phone;

    protected CustomerCollaboratorRef() {
        // for ORM
    }

    public CustomerCollaboratorRef(final String username, final String phone) {
        Objects.requireNonNull(username, "Username is required");
        final var normalizedUsername = username.strip();
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        this.username = normalizedUsername;
        this.phone = new CollaboratorPhoneNumber(phone).value();
    }

    public String username() {
        return username;
    }

    public String phone() {
        return phone;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (CustomerCollaboratorRef) o;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
