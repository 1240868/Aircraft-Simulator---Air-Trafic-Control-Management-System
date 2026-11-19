package eapli.aisafe.pilot;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PilotCertification implements ValueObject {
    @Column(name = "certification", nullable = false, length = 100)
    private String value;

    protected PilotCertification() {
    }

    public PilotCertification(final String value) {
        Preconditions.nonEmpty(value, "Pilot certification is required");
        this.value = value.trim();
    }

    public String value() { return value; }
}
