package eapli.aisafe.pilot;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class License implements ValueObject {
    @Column(name = "license_number", nullable = false, length = 50)
    private String value;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    protected License() {
    }

    public License(final String value, final LocalDateTime expirationDate) {
        Preconditions.nonEmpty(value, "License is required");
        Preconditions.nonNull(expirationDate, "Expiration date is required");
        if (expirationDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }
        this.value = value.trim();
        this.expirationDate = expirationDate;
    }

    public String value() { return value; }
    public LocalDateTime expirationDate() { return expirationDate; }
}
