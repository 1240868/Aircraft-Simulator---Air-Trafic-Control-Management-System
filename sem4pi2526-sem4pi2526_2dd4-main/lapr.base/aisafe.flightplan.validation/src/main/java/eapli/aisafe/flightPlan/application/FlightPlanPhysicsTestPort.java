package eapli.aisafe.flightPlan.application;

import java.nio.file.Path;
import java.util.List;

/** US085 — C-based physics validation (fuel, altitude). */
public interface FlightPlanPhysicsTestPort {

    List<String> run(Path inputJson);
}
