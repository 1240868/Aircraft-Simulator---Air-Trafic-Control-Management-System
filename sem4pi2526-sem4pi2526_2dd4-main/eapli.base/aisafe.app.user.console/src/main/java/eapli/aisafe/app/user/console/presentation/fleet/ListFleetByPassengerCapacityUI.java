package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.FleetListResult;
import eapli.aisafe.fleet.application.ListFleetByPassengerCapacityController;
import eapli.framework.io.util.Console;

public class ListFleetByPassengerCapacityUI extends AbstractBaseUI {

    private final ListFleetByPassengerCapacityController controller = new ListFleetByPassengerCapacityController();

    @Override
    protected boolean doShow() {
        final String capacityRaw = Console.readLine("Total passenger capacity to list (positive integer): ");
        try {
            final FleetListResult r = controller.listFleetByPassengerCapacity(capacityRaw);
            if (r.messageWhenEmpty() != null) {
                System.out.println(r.messageWhenEmpty());
                return false;
            }
            System.out.println("\n--- Aircraft (capacity match, active fleet) ---");
            for (final var row : r.aircraft()) {
                System.out.printf("  %s | %s | %s | %s | seats=%d%n",
                        row.registrationNumber(),
                        row.modelName(),
                        row.manufacturer(),
                        row.operationalStatus(),
                        row.numberOfSeats());
            }
            System.out.println("----------------------------------------------");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > List fleet by passenger capacity";
    }
}
