package eapli.aisafe.flightPlan.application;

import eapli.aisafe.flightPlan.domain.FlightPlan;
import java.io.IOException;
import java.nio.file.Path;

/** US085 — writes the JSON input consumed by {@code flight_plan_validator}. */
@FunctionalInterface
public interface FlightPlanTestPayloadWriter {

    Path writePayload(FlightPlan flightPlan) throws IOException;
}
