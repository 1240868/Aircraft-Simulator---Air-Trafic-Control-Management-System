package eapli.aisafe.app.backoffice.console.presentation.company;

import eapli.aisafe.aircontrol.application.RegisterAirControlAreaController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class RegisterAirControlAreaUI extends AbstractUI {

    private final RegisterAirControlAreaController controller = new RegisterAirControlAreaController();

    @Override
    protected boolean doShow() {
        final String code = Console.readLine("Area code (e.g. PT-LIS)");
        final String name = Console.readLine("Area display name");
        final double latMin = Console.readDouble("Latitude min");
        final double latMax = Console.readDouble("Latitude max");
        final double lonMin = Console.readDouble("Longitude min");
        final double lonMax = Console.readDouble("Longitude max");

        try {
            final var area = controller.registerAirControlArea(code, name, latMin, latMax, lonMin, lonMax);
            System.out.println("Registered: " + area.name()
                    + " code=" + area.areaCode()
                    + " identity=" + area.identity());
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final Exception ex) {
            System.out.println("Invalid input: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register air control area";
    }
}
