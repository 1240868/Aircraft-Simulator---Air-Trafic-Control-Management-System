package eapli.aisafe.company.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class AirOperatorCertificate implements ValueObject {
    @Column(name = "aoc_number", nullable = false, length = 64)
    private String number;

    @Column(name = "aoc_expiry", nullable = false)
    private LocalDate expiryDate;

    protected AirOperatorCertificate() {
        // for ORM
    }

    public AirOperatorCertificate(final String number, final LocalDate expiryDate) {
        Preconditions.nonEmpty(number, "Certificate number is required");
        this.number = number.strip();
        this.expiryDate = java.util.Objects.requireNonNull(expiryDate, "Certificate expiry is required");
    }

    public String number() {
        return number;
    }

    public LocalDate expiryDate() {
        return expiryDate;
    }
}
