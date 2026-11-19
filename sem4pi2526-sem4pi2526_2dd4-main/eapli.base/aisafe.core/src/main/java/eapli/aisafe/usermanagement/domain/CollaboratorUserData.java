package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import java.util.Objects;

/**
 * Validated collaborator identity data used when registering a system user.
 */
public final class CollaboratorUserData implements ValueObject {

    private final CollaboratorEmail email;
    private final CollaboratorFullName fullName;
    private final CollaboratorPhoneNumber phone;

    public CollaboratorUserData(final CollaboratorEmail email,
                                final CollaboratorFullName fullName,
                                final CollaboratorPhoneNumber phone) {
        this.email = Objects.requireNonNull(email, "Email is required");
        this.fullName = Objects.requireNonNull(fullName, "Full name is required");
        this.phone = Objects.requireNonNull(phone, "Phone is required");
    }

    public CollaboratorEmail email() {
        return email;
    }

    public CollaboratorFullName fullName() {
        return fullName;
    }

    public CollaboratorPhoneNumber phone() {
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
        final var that = (CollaboratorUserData) o;
        return email.equals(that.email) && fullName.equals(that.fullName) && phone.equals(that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, fullName, phone);
    }
}
