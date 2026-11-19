package eapli.aisafe.airport.domain;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "airports")
public class Airport implements AggregateRoot<String> {

    @Id
    @Column(name = "icao_code", nullable = false, length = 4)
    private String icaoCode;

    @Column(name = "iata_code", nullable = false, length = 3, unique = true)
    private String iataCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String town;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double elevationMeters;

    @ManyToOne
    private AirControlArea airControlArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 32)
    private OperationalStatus operationalStatus;

    protected Airport() {
    }

    public Airport(final String icaoCode, final String iataCode, final String name,
                   final String town, final String country,
                   final double latitude, final double longitude,
                   final double elevationMeters, final AirControlArea airControlArea) {
        Preconditions.nonEmpty(icaoCode, "ICAO code is required");
        Preconditions.ensure(icaoCode.trim().length() == 4, "ICAO code must be 4 characters");
        Preconditions.nonEmpty(iataCode, "IATA code is required");
        Preconditions.ensure(iataCode.trim().length() == 3, "IATA code must be 3 characters");
        Preconditions.nonEmpty(name, "Name is required");
        Preconditions.nonEmpty(town, "Town is required");
        Preconditions.nonEmpty(country, "Country is required");
        Preconditions.ensure(latitude >= -90 && latitude <= 90, "Invalid latitude");
        Preconditions.ensure(longitude >= -180 && longitude <= 180, "Invalid longitude");
        Preconditions.noneNull(airControlArea);

        this.icaoCode = icaoCode.trim().toUpperCase();
        this.iataCode = iataCode.trim().toUpperCase();
        this.name = name;
        this.town = town;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationMeters = elevationMeters;
        this.airControlArea = airControlArea;
        this.operationalStatus = OperationalStatus.ACTIVE;
    }

    @Override
    public String identity() { return icaoCode; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof Airport)) return false;
        return identity().equals(((Airport) other).identity());
    }

    public String icaoCode() { return icaoCode; }
    public String iataCode() { return iataCode; }
    public String name() { return name; }
    public String town() { return town; }
    public String country() { return country; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
    public double elevationMeters() { return elevationMeters; }
    public AirControlArea airControlArea() { return airControlArea; }

    public OperationalStatus operationalStatus() {
        return operationalStatus;
    }

    public boolean isOperational() {
        return operationalStatus == OperationalStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return icaoCode + "/" + iataCode + " - " + name;
    }
}
