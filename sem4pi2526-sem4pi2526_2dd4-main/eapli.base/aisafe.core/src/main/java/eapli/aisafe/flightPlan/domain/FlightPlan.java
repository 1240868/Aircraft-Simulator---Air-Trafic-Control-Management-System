package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightroute.FlightRouteId;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "flight_plans")
public class FlightPlan implements AggregateRoot<FlightPlanId> {

    @EmbeddedId
    private FlightPlanId id;

    @Embedded
    private RegistrationNumber aircraft;

    @Embedded
    private FlightRouteId flightRoute;

    @Embedded
    private PilotId pilot;

    @Embedded
    private FlightPlanDesignator designator;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FlightPlanStatus status;

    @Column(name = "flight_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FlightType flightType;

    @Embedded
    private DepartureDateTime departureDateTime;

    @OneToMany(mappedBy = "flightPlan", cascade = CascadeType.MERGE, orphanRemoval = true)
    private List<FlightLeg> legs;

    @Embedded
    private FlightWind wind;

    @Column(name = "dsl_source_path", length = 512)
    private String dslSourcePath;

    protected FlightPlan() {
    }

    public FlightPlan(final FlightPlanId id, final RegistrationNumber aircraft, final FlightRouteId flightRoute,
                      final FlightPlanDesignator designator, final FlightType flightType,
                      final DepartureDateTime departureDateTime, final List<FlightLeg> legs,
                      final FlightWind wind) {

        Preconditions.ensure(legs != null && !legs.isEmpty(), "At least one leg is required");

        this.id = id;
        this.aircraft = aircraft;
        this.flightRoute = flightRoute;
        this.designator = designator;
        this.status = FlightPlanStatus.DRAFT;
        this.flightType = flightType;
        this.departureDateTime = departureDateTime;
        this.legs = legs;
        for (final FlightLeg leg : legs) {
            leg.setFlightPlan(this);
        }
        this.wind = wind;
    }

    @Override
    public String toString() {
        return "FlightPlan{" +
                "id=" + id +
                ", aircraft=" + aircraft +
                ", flightRoute=" + flightRoute +
                ", designator=" + designator +
                ", status=" + status +
                ", flightType=" + flightType +
                ", departureDateTime=" + departureDateTime +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightPlan that = (FlightPlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightPlan)) return false;
        final FlightPlan that = (FlightPlan) other;
        return this != that && Objects.equals(this.id, that.id);
    }

    @Override
    public FlightPlanId identity() {
        return id;
    }

    @Override
    public int compareTo(final FlightPlanId other) {
        return AggregateRoot.super.compareTo(other);
    }

    public FlightPlanId getId() { return id; }
    public void setId(final FlightPlanId id) { this.id = id; }
    public RegistrationNumber getAircraft() { return aircraft; }
    public void setAircraft(final RegistrationNumber aircraft) { this.aircraft = aircraft; }
    public FlightRouteId getFlightRoute() { return flightRoute; }
    public void setFlightRoute(final FlightRouteId flightRoute) { this.flightRoute = flightRoute; }
    public FlightPlanDesignator getDesignator() { return designator; }
    public void setDesignator(final FlightPlanDesignator designator) { this.designator = designator; }
    public FlightPlanStatus getStatus() { return status; }
    public void setStatus(final FlightPlanStatus status) { this.status = status; }
    public FlightType getFlightType() { return flightType; }
    public void setFlightType(final FlightType flightType) { this.flightType = flightType; }
    public DepartureDateTime getDepartureDateTime() { return departureDateTime; }
    public void setDepartureDateTime(final DepartureDateTime departureDateTime) { this.departureDateTime = departureDateTime; }
    public List<FlightLeg> getLegs() { return legs; }
    public void setLegs(final List<FlightLeg> legs) { this.legs = legs; }
    public FlightWind getWind() { return wind; }
    public void setWind(final FlightWind wind) { this.wind = wind; }
    public PilotId getPilot() {
        return pilot;
    }
    public void setPilot(PilotId pilot) {
        this.pilot = pilot;
    }

    public void insertWeatherData(final WeatherData weatherData, final FlightLegId legId) {
        if (status == FlightPlanStatus.CANCELLED || status == FlightPlanStatus.COMPLETED) {
            throw new IllegalStateException("Cannot add weather data to a " + status + " flight plan");
        }

        FlightLeg target = null;
        for (final FlightLeg leg : legs) {
            if (leg.identity().equals(legId)) {
                target = leg;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Leg not found: " + legId);
        }

        if (status == FlightPlanStatus.TESTED) {
            this.status = FlightPlanStatus.PENDING_TEST;
        }

        target.assignWeatherData(weatherData);
    }

    public void updateWeatherData(final WindSpeed speed, final WindDirection direction) {
        this.wind = new FlightWind(speed, direction, MeasurementUnit.KILOGRAM);
    }

    public String getDslSourcePath() {
        return dslSourcePath;
    }

    public void setDslSourcePath(final String dslSourcePath) {
        this.dslSourcePath = dslSourcePath;
    }

    public void markTestPassed() {
        this.status = FlightPlanStatus.TESTED;
    }

    public void markTestFailed() {
        this.status = FlightPlanStatus.REJECTED;
    }

}
