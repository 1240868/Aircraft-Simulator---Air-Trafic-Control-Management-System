package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.AddAircraftController;
import eapli.aisafe.fleet.application.AircraftModelOptionDTO;
import eapli.aisafe.fleet.application.FleetListResult;
import eapli.aisafe.fleet.application.ListFleetByModelController;
import eapli.framework.io.util.Console;
import java.util.List;

public class ListFleetByModelUI extends AbstractBaseUI {

    private final AddAircraftController modelsController = new AddAircraftController();
    private final ListFleetByModelController listController = new ListFleetByModelController();

    @Override
    protected boolean doShow() {
        final List<AircraftModelOptionDTO> models = modelsController.listAircraftModels();
        if (models.isEmpty()) {
            System.out.println("No aircraft models available.");
            return false;
        }

        System.out.println("\n--- Aircraft models (select one to list your fleet) ---");
        int idx = 1;
        for (final AircraftModelOptionDTO dto : models) {
            System.out.printf("  %d. %s / %s  [%s]  capacity=%d  (id=%s)%n",
                    idx++, dto.modelName(), dto.manufacturer(), dto.type(),
                    dto.passengerCapacity(), dto.modelId());
        }
        System.out.println("--------------------------------------------------------");

        final int choice = Console.readInteger("Select model number");
        if (choice < 1 || choice > models.size()) {
            System.out.println("Invalid selection.");
            return false;
        }
        final AircraftModelOptionDTO selected = models.get(choice - 1);

        try {
            final FleetListResult r = listController.listFleetByModel(selected.modelId());
            if (r.messageWhenEmpty() != null) {
                System.out.println(r.messageWhenEmpty());
                return false;
            }
            System.out.println("\n--- Aircraft (selected model, active fleet) ---");
            for (final var row : r.aircraft()) {
                System.out.printf("  %s | %s | %s | %s | seats=%d%n",
                        row.registrationNumber(),
                        row.modelName(),
                        row.manufacturer(),
                        row.operationalStatus(),
                        row.numberOfSeats());
            }
            System.out.println("-----------------------------------------------");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > List fleet by aircraft model";
    }
}
