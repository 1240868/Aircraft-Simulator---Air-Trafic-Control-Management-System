package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.FleetListResult;
import eapli.aisafe.fleet.application.ListFleetByMakerController;
import eapli.framework.io.util.Console;

public class ListFleetByMakerUI extends AbstractBaseUI {

    private final ListFleetByMakerController controller = new ListFleetByMakerController();

    @Override
    protected boolean doShow() {
        final String manufacturerRaw = Console.readLine("Aircraft manufacturer to filter (e.g. Airbus, Boeing): ");
        try {
            final FleetListResult r = controller.listFleetByMaker(manufacturerRaw);
            if (r.messageWhenEmpty() != null) {
                System.out.println(r.messageWhenEmpty());
                return false;
            }
            System.out.println("\n--- Aircraft (manufacturer match, active fleet) ---");
            for (final var row : r.aircraft()) {
                System.out.printf("  %s | %s | %s | %s | seats=%d%n",
                        row.registrationNumber(),
                        row.modelName(),
                        row.manufacturer(),
                        row.operationalStatus(),
                        row.numberOfSeats());
            }
            System.out.println("-----------------------------------------------------");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > List fleet by manufacturer";
    }
}
