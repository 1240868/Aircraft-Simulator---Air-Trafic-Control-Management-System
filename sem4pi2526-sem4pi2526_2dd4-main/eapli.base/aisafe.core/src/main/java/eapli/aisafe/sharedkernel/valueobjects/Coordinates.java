package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;

@Embeddable
public class Coordinates implements ValueObject {
    private double latitude;
    private double longitude;

    protected Coordinates() {
    }

    public Coordinates(final double latitude, final double longitude) {
        Preconditions.ensure(latitude >= -90 && latitude <= 90, "Latitude must be between -90 and 90");
        Preconditions.ensure(longitude >= -180 && longitude <= 180, "Longitude must be between -180 and 180");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
}
