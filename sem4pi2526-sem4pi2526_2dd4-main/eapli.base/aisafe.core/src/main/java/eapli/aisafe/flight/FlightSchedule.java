package eapli.aisafe.flight;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class FlightSchedule implements ValueObject {
    private LocalDateTime departure;
    private LocalDateTime arrival;

    protected FlightSchedule() {
    }

    public FlightSchedule(final LocalDateTime departure, final LocalDateTime arrival) {
        Preconditions.nonNull(departure, "Departure time is required");
        Preconditions.nonNull(arrival, "Arrival time is required");
        Preconditions.ensure(arrival.isAfter(departure), "Arrival must be after departure");
        this.departure = departure;
        this.arrival = arrival;
    }

    public LocalDateTime departure() { return departure; }
    public LocalDateTime arrival() { return arrival; }
}
