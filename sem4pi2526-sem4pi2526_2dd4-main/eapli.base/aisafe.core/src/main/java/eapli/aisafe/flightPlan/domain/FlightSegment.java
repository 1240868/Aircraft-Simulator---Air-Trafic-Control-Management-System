package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.Altitude;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntity;
import jakarta.persistence.*;
import org.springframework.data.geo.Distance;

import java.time.Duration;

@Entity
@Table(name = "flight_segment")
public class FlightSegment implements DomainEntity<FlightSegmentId> {

    @EmbeddedId
    private FlightSegmentId id;

    @Embedded
    private Altitude altitude;

    @Column(name = "segment_duration")
    private Duration duration;

    @Column(name = "segment_distance")
    private Distance distance;

    @Embedded
    private FlightWind wind;

    protected FlightSegment() {
    }

    public FlightSegment(final FlightSegmentId id, final Distance distance,
                         final Duration duration, final Altitude altitude) {
        this.id = id;
        this.distance = distance;
        this.duration = duration;
        this.altitude = altitude;
    }

    public void updateWeatherData(final FlightWind wind) {
        this.wind = wind;
    }

    @Override
    public FlightSegmentId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightSegment)) return false;
        final FlightSegment that = (FlightSegment) other;
        return id.equals(that.id);
    }

    public Distance distance() { return distance; }
    public Duration duration() { return duration; }
    public Altitude altitude() { return altitude; }
    public FlightWind wind() { return wind; }
}