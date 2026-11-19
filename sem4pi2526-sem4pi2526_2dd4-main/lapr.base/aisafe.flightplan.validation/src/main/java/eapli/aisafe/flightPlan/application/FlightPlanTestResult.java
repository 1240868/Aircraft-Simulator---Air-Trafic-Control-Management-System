package eapli.aisafe.flightPlan.application;

import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import java.util.List;

/** US085 — outcome of a flight plan validation/test run. */
public record FlightPlanTestResult(
        boolean passed,
        List<String> dslErrors,
        List<String> physicsErrors,
        FlightPlanStatus newStatus) {

    public static FlightPlanTestResult success() {
        return new FlightPlanTestResult(true, List.of(), List.of(), FlightPlanStatus.TESTED);
    }

    public static FlightPlanTestResult failed(final List<String> dslErrors,
                                              final List<String> physicsErrors,
                                              final FlightPlanStatus status) {
        return new FlightPlanTestResult(false, dslErrors, physicsErrors, status);
    }
}
