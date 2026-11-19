package eapli.aisafe.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

@Embeddable
public class FlightRouteName implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}\\d{1,4}$");

    @Column(name = "route_name", nullable = false, length = 100, unique = true)
    private String value;

    protected FlightRouteName() {
    }

    public FlightRouteName(final String value) {
        Preconditions.nonEmpty(value, "Flight route name is required");
        final String normalized = value.trim().toUpperCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid route name: " + value + ". Expected 2 letters + 1-4 digits (e.g. TP123)");
        }
        this.value = normalized;
    }

    public static FlightRouteName compose(final String iataPrefix, final String numericSuffix) {
        Preconditions.nonEmpty(iataPrefix, "IATA prefix is required");
        Preconditions.nonEmpty(numericSuffix, "Numeric suffix is required");
        final String suffix = numericSuffix.trim();
        if (!suffix.matches("\\d{1,4}")) {
            throw new IllegalArgumentException("Numeric suffix must contain 1 to 4 digits");
        }
        return new FlightRouteName(iataPrefix.trim().toUpperCase() + suffix);
    }

    public String value() {
        return value;
    }
}
