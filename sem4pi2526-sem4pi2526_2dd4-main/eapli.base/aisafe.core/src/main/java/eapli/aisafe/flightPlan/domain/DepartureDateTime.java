package eapli.aisafe.flightPlan.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class DepartureDateTime implements ValueObject {
    @Column(name = "departure_date_time", nullable = false)
    private LocalDateTime value;

    protected DepartureDateTime() {
    }

    public DepartureDateTime(final LocalDateTime value) {
        Preconditions.nonNull(value, "Departure date/time is required");
        this.value = value;
    }

    public LocalDateTime value() { return value; }
}
