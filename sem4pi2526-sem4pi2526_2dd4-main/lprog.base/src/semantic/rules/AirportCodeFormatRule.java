package semantic.rules;

import domain.AirportCode;
import domain.FlightPlan;
import domain.Leg;
import semantic.SemanticError;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AirportCodeFormatRule implements SemanticRule {

    private static final Pattern IATA = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern ICAO = Pattern.compile("^[A-Z]{4}$");

    @Override
    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> errors = new ArrayList<>();
        for (Leg leg : plan.flight().legs()) {
            check(leg.departureAirport(), errors);
            check(leg.arrivalAirport(), errors);
        }
        return errors;
    }

    private void check(AirportCode a, List<SemanticError> errors) {
        String c = a.code();
        if (!IATA.matcher(c).matches() && !ICAO.matcher(c).matches()) {
            errors.add(new SemanticError(
                    "AirportCodeFormat",
                    a.line(),
                    "Airport code '" + c + "' must be IATA (3 uppercase letters) or ICAO (4 uppercase letters)"
            ));
        }
    }
}