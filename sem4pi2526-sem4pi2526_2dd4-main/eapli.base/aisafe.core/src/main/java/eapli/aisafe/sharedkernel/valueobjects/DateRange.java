package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class DateRange implements ValueObject {
    private LocalDateTime start;
    private LocalDateTime end;

    protected DateRange() {
    }

    public DateRange(final LocalDateTime start, final LocalDateTime end) {
        Preconditions.nonNull(start, "Start date is required");
        Preconditions.nonNull(end, "End date is required");
        Preconditions.ensure(end.isAfter(start), "End date must be after start date");
        this.start = start;
        this.end = end;
    }

    public LocalDateTime start() { return start; }
    public LocalDateTime end() { return end; }
}
