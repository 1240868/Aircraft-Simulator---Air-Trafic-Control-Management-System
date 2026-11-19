package eapli.aisafe.app.user.console.presentation.pilot;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.application.dto.PilotRosterListItemDTO;
import eapli.aisafe.pilot.application.dto.PilotRosterListResult;

public class ListPilotRosterUI extends AbstractBaseUI {

    private final ListPilotRosterController controller = new ListPilotRosterController();

    @Override
    protected boolean doShow() {
        try {
            final PilotRosterListResult result = controller.listPilotRoster();
            if (result.messageWhenEmpty() != null) {
                System.out.println(result.messageWhenEmpty());
                return false;
            }
            System.out.println("\n--- Company pilot roster (active only) ---");
            for (final PilotRosterListItemDTO row : result.pilots()) {
                System.out.printf(
                        "  %s | %s | %s | %s | expires %s | %s | certified: %s%n",
                        row.fullName(),
                        row.email(),
                        row.certification(),
                        row.licenseNumber(),
                        row.licenseExpiration(),
                        row.status(),
                        String.join(", ", row.certifiedModels()));
            }
            System.out.println("------------------------------------------");
        } catch (final IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > List company pilot roster";
    }
}
