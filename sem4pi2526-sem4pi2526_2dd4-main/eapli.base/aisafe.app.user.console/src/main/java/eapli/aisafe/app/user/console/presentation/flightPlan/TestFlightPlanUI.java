package eapli.aisafe.app.user.console.presentation.flightPlan;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.flightPlan.application.FlightPlanTestResult;
import eapli.aisafe.flightPlan.application.TestFlightPlanController;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.framework.io.util.Console;

import java.util.List;

/** US085 — console UI for testing/validating a flight plan. */
public class TestFlightPlanUI extends AbstractBaseUI {

    private final TestFlightPlanController controller = new TestFlightPlanController();

    @Override
    protected boolean doShow() {
        try {
            final List<FlightPlan> plans = controller.listTestablePlansForCurrentPilot();
            if (plans.isEmpty()) {
                System.out.println("No testable flight plans found for your pilot profile.");
                System.out.println("(Plans must be DRAFT, PENDING_TEST or REJECTED and assigned to you.)");
                return false;
            }

            System.out.println("\n--- Testable Flight Plans ---");
            int idx = 1;
            for (final FlightPlan plan : plans) {
                System.out.printf("  %d. %s  id=%s  status=%s%n",
                        idx++,
                        plan.getDesignator(),
                        plan.getId(),
                        plan.getStatus());
            }
            System.out.println("-----------------------------");

            final int choice = Console.readInteger("Select flight plan number");
            if (choice < 1 || choice > plans.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final FlightPlan selected = plans.get(choice - 1);
            System.out.println("Running DSL + physics validation...");

            final FlightPlanTestResult result =
                    controller.testForCurrentPilot(selected.getId());

            if (result.passed()) {
                System.out.println("Flight plan PASSED validation. Status: " + result.newStatus());
            } else {
                System.out.println("Flight plan FAILED validation. Status: " + result.newStatus());
                if (!result.dslErrors().isEmpty()) {
                    System.out.println("DSL errors:");
                    result.dslErrors().forEach(err -> System.out.println("  - " + err));
                }
                if (!result.physicsErrors().isEmpty()) {
                    System.out.println("Physics errors:");
                    result.physicsErrors().forEach(err -> System.out.println("  - " + err));
                }
            }
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Test / Validate Flight Plan";
    }
}
