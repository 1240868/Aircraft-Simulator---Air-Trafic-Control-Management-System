package eapli.aisafe.flightroute;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.company.domain.CompanyId;
import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "flight_routes")
public class FlightRoute implements AggregateRoot<FlightRouteId> {

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RouteActivationStatus status;

    @EmbeddedId
    private FlightRouteId id;

    @Embedded
    private FlightRouteName name;

    @Embedded
    private CompanyId companyId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "origin_airport_id", length = 36))
    private AirportId originAirportId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "dest_airport_id", length = 36))
    private AirportId destinationAirportId;

    @Embedded
    private RouteDistance distance;

    @Embedded
    private RouteDuration duration;

    protected FlightRoute() {
    }

    public static FlightRoute create(final FlightRouteName name,
                                     final CompanyId companyId,
                                     final AirportId originAirportId,
                                     final AirportId destinationAirportId,
                                     final RouteDistance distance,
                                     final RouteDuration duration) {
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(companyId, "Company is required");
        Objects.requireNonNull(originAirportId, "Origin airport is required");
        Objects.requireNonNull(destinationAirportId, "Destination airport is required");
        Objects.requireNonNull(distance, "Distance is required");
        Objects.requireNonNull(duration, "Duration is required");
        if (originAirportId.equals(destinationAirportId)) {
            throw new IllegalArgumentException("Origin airport must differ from destination airport");
        }
        return new FlightRoute(
                FlightRouteId.newId(),
                name,
                companyId,
                originAirportId,
                destinationAirportId,
                distance,
                duration);
    }

    private FlightRoute(final FlightRouteId id,
                        final FlightRouteName name,
                        final CompanyId companyId,
                        final AirportId originAirportId,
                        final AirportId destinationAirportId,
                        final RouteDistance distance,
                        final RouteDuration duration) {
        this.id = id;
        this.name = name;
        this.companyId = companyId;
        this.originAirportId = originAirportId;
        this.destinationAirportId = destinationAirportId;
        this.distance = distance;
        this.duration = duration;
        this.status = RouteActivationStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == RouteActivationStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "FlightRoute{" +
                "status=" + status +
                ", id=" + id +
                ", name=" + name +
                ", distance=" + distance +
                ", duration=" + duration +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightRoute that = (FlightRoute) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightRoute)) return false;
        final FlightRoute that = (FlightRoute) other;
        return this != that && Objects.equals(this.id, that.id);
    }

    @Override
    public FlightRouteId identity() {
        return id;
    }

    @Override
    public int compareTo(final FlightRouteId other) {
        return AggregateRoot.super.compareTo(other);
    }

    public RouteActivationStatus getStatus() { return status; }
    public FlightRouteId getId() { return id; }
    public FlightRouteName getName() { return name; }
    public CompanyId companyId() { return companyId; }
    public AirportId originAirportId() { return originAirportId; }
    public AirportId destinationAirportId() { return destinationAirportId; }
    public RouteDistance getDistance() { return distance; }
    public RouteDuration getDuration() { return duration; }

}
