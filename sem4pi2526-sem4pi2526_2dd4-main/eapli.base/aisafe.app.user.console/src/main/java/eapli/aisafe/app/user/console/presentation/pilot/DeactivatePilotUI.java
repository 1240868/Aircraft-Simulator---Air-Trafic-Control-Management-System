package eapli.aisafe.app.user.console.presentation.pilot;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.application.DeactivatePilotController;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.application.dto.PilotRosterListItemDTO;
import eapli.aisafe.pilot.application.dto.PilotRosterListResult;
import eapli.framework.io.util.Console;

import java.util.List;
import java.util.UUID;

public class DeactivatePilotUI extends AbstractBaseUI {

    private final ListPilotRosterController listController = new ListPilotRosterController();
    private final DeactivatePilotController deactivateController = new DeactivatePilotController();

    @Override
    protected boolean doShow() {
        try {
            final PilotRosterListResult roster = listController.listPilotRoster();
            if (roster.messageWhenEmpty() != null) {
                System.out.println(roster.messageWhenEmpty());
                return false;
            }

            final List<PilotRosterListItemDTO> pilots = roster.pilots();
            System.out.println("\n--- Active pilots (select one to deactivate) ---");
            int idx = 1;
            for (final PilotRosterListItemDTO row : pilots) {
                System.out.printf("  %d. %s | %s | %s%n", idx++, row.fullName(), row.email(), row.licenseNumber());
            }
            System.out.println("------------------------------------------------");

            final int choice = Console.readInteger("Select pilot number");
            if (choice < 1 || choice > pilots.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final PilotRosterListItemDTO selected = pilots.get(choice - 1);
            deactivateController.deactivatePilot(new PilotId(UUID.fromString(selected.pilotId())));
            System.out.println("Pilot " + selected.fullName() + " has been deactivated and removed from the active roster.");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Remove pilot from roster";
    }
}
