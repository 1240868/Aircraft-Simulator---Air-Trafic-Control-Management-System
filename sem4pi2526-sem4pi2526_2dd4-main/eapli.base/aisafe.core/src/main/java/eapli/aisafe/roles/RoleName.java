package eapli.aisafe.roles;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class RoleName implements ValueObject {
    private String value;

    protected RoleName() {
    }

    public RoleName(final String value) {
        Preconditions.nonEmpty(value, "Role name is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
