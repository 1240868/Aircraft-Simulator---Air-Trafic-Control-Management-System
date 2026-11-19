package eapli.aisafe.app.backoffice.console.presentation.aircontrol;

import eapli.aisafe.aircontrol.application.RegisterAirControlAreaController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirControlAreaUI extends AbstractUI {

    private final RegisterAirControlAreaController controller = new RegisterAirControlAreaController();

    @Override
    protected boolean doShow() {
        final String code = Console.readLine("Area Code (e.g. LPPT)");
        final String name = Console.readLine("Area Name");
        final double latMin = Console.readDouble("Latitude Min (-90 to 90)");
        final double latMax = Console.readDouble("Latitude Max (-90 to 90)");
        final double lonMin = Console.readDouble("Longitude Min (-180 to 180)");
        final double lonMax = Console.readDouble("Longitude Max (-180 to 180)");

        try {
            controller.registerAirControlArea(code, name, latMin, latMax, lonMin, lonMax);
            System.out.println("Air control area registered successfully.");
        } catch (final IntegrityViolationException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register Air Control Area";
    }
}
