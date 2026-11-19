package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FullName implements ValueObject {
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    protected FullName() {
    }

    public FullName(final String firstName, final String lastName) {
        Preconditions.nonEmpty(firstName, "First name is required");
        Preconditions.nonEmpty(lastName, "Last name is required");
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
}
