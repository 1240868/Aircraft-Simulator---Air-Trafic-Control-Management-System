package eapli.aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class AirportLocation implements ValueObject {
    private String city;
    private String country;

    protected AirportLocation() {
    }

    @Override
    public String toString() {
        return "AirportLocation{" +
                "city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }

    public String getCity() {
        return city;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AirportLocation that = (AirportLocation) o;
        return Objects.equals(city, that.city) && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, country);
    }


    public String getCountry() {
        return country;
    }


    public AirportLocation(final String city, final String country) {
        Preconditions.nonEmpty(city, "City is required");
        Preconditions.nonEmpty(country, "Country is required");
        this.city = city.trim();
        this.country = country.trim();
    }

    public String city() { return city; }
    public String country() { return country; }
}
