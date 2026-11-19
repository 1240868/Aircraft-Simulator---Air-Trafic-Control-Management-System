package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.flightPlan.domain.FlightSegmentId;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "flight_legs")
public class FlightLeg implements DomainEntity<FlightLegId> {

    @EmbeddedId
    private FlightLegId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "departure_airport_id"))
    private AirportId departureAirport;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "arrival_airport_id"))
    private AirportId arrivalAirport;

    @Column(name = "leg_order", nullable = false)
    private int legOrder;

    @ElementCollection
    @CollectionTable(name = "flight_leg_segments",
            joinColumns = @JoinColumn(name = "flight_leg_id"))
    private List<FlightSegmentId> segments;

    @Embedded
    private FuelPlan fuelPlan;

    @ManyToOne
    @JoinColumn(name = "flight_plan_id")
    private FlightPlan flightPlan;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "weather_data_id")
    private WeatherData weatherData;

    protected FlightLeg() {
    }

    public FlightLeg(final FlightLegId id, final AirportId departureAirport,
                     final AirportId arrivalAirport, final int legOrder,
                     final List<FlightSegmentId> segments,
                     final FuelPlan fuelPlan) {
        Preconditions.noneNull(departureAirport, "Departure airport is required");
        Preconditions.noneNull(arrivalAirport, "Arrival airport is required");
        Preconditions.ensure(!departureAirport.equals(arrivalAirport), "Departure and arrival airports must be different");
        Preconditions.ensure(legOrder > 0, "Leg order must be positive");
        Preconditions.nonNull(segments, "Segments list is required");
        Preconditions.ensure(!segments.isEmpty(), "At least one segment is required");
        Preconditions.noneNull(fuelPlan, "Fuel plan is required");

        this.id = id;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.legOrder = legOrder;
        this.segments = segments;
        this.fuelPlan = fuelPlan;
    }

    public AirportId departureAirport() { return departureAirport; }
    public AirportId arrivalAirport() { return arrivalAirport; }
    public int legOrder() { return legOrder; }
    public List<FlightSegmentId> segments() { return segments; }
    public FuelPlan fuelPlan() { return fuelPlan; }
    public WeatherData weatherData() { return weatherData; }
    public FlightPlan flightPlan() { return flightPlan; }

    public void setFlightPlan(final FlightPlan flightPlan) {
        this.flightPlan = flightPlan;
    }

    public void assignWeatherData(final WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    @Override
    public FlightLegId identity() { return id; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlightLeg that = (FlightLeg) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightLeg)) return false;
        final FlightLeg that = (FlightLeg) other;
        return this != other && Objects.equals(this.id, that.id);
    }
}
