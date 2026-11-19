package eapli.aisafe.flightPlan.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FlightPlanDesignator implements ValueObject {
    @Column(name = "designator", nullable = false, length = 20)
    private String value;

    protected FlightPlanDesignator() {
    }

    public FlightPlanDesignator(final String value) {
        Preconditions.nonEmpty(value, "Flight plan designator is required");
        Preconditions.noneNull(value, "Designator is required");
        this.value = value.trim();
    }

    public String value() { return value; }
    public String getLetters() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                res.append(value.charAt(i));
            } else {
                break;
            }
        }
        return res.toString();
    }
}
