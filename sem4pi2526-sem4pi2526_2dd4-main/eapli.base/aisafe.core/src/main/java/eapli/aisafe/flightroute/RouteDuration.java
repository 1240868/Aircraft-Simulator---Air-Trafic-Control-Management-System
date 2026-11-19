package eapli.aisafe.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RouteDuration implements ValueObject {
    @Column(name = "duration_minutes", nullable = false)
    private long minutes;

    protected RouteDuration() {
    }

    public RouteDuration(final long minutes) {
        Preconditions.ensure(minutes > 0, "Duration must be positive");
        this.minutes = minutes;
    }

    public long minutes() {
        return minutes;
    }
}
