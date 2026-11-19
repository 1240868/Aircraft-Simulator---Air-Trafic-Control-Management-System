package eapli.aisafe.flightPlan.application;

import eapli.aisafe.flightPlan.domain.FlightPlan;
import java.util.List;

/** US085 — lexical/syntactic/semantic DSL validation (LPROG integration point). */
public interface FlightPlanDslValidationPort {

    List<String> validate(FlightPlan flightPlan);
}
