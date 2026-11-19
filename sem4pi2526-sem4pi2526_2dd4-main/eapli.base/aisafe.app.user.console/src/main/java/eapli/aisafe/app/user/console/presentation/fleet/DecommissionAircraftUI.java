package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.DecommissionAircraftController;
import eapli.framework.io.util.Console;

public class DecommissionAircraftUI extends AbstractBaseUI {

    private final DecommissionAircraftController controller = new DecommissionAircraftController();

    @Override
    protected boolean doShow() {
        final String registration = Console.readLine("Aircraft registration (tail number): ");
        try {
            controller.decommission(registration);
            System.out.println("Aircraft " + registration.strip().toUpperCase() + " has been retired from the fleet.");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Decommission aircraft";
    }
}
