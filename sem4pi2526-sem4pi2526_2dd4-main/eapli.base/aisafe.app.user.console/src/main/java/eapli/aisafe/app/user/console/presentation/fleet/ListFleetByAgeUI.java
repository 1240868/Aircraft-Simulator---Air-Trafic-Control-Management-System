package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.FleetListResult;
import eapli.aisafe.fleet.application.ListFleetByAgeController;
import eapli.framework.io.util.Console;

public class ListFleetByAgeUI extends AbstractBaseUI {

    private final ListFleetByAgeController controller = new ListFleetByAgeController();

    @Override
    protected boolean doShow() {
        final String ageRaw = Console.readLine("Aircraft age to list (complete years, positive integer): ");
        try {
            final FleetListResult r = controller.listFleetByAge(ageRaw);
            if (r.messageWhenEmpty() != null) {
                System.out.println(r.messageWhenEmpty());
                return false;
            }
            System.out.println("\n--- Aircraft (age match, active fleet) ---");
            for (final var row : r.aircraft()) {
                System.out.printf("  %s | %s | %s | %s | seats=%d | age=%d years%n",
                        row.registrationNumber(),
                        row.modelName(),
                        row.manufacturer(),
                        row.operationalStatus(),
                        row.numberOfSeats(),
                        row.ageInCompleteYears());
            }
            System.out.println("------------------------------------------");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > List fleet by aircraft age";
    }
}
