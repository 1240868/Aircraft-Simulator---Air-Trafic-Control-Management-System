package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Email implements ValueObject {
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String value;

    protected Email() {
    }

    public Email(final String value) {
        Preconditions.nonEmpty(value, "Email is required");
        Preconditions.ensure(value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Invalid email format");
        this.value = value.toLowerCase().trim();
    }

    public String value() { return value; }
}
