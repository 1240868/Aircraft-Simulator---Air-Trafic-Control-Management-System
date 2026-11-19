package eapli.aisafe.app.backoffice.console.presentation.airport;

import eapli.aisafe.airport.application.RegisterAirportController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirportUI extends AbstractUI {

    private final RegisterAirportController controller = new RegisterAirportController();

    @Override
    protected boolean doShow() {
        System.out.println("Available Air Control Areas:");
        controller.listAirControlAreas().forEach(a -> System.out.println("  " + a));

        final String icaoCode = Console.readLine("ICAO Code (4 letters)");
        final String iataCode = Console.readLine("IATA Code (3 letters)");
        final String name = Console.readLine("Airport Name");
        final String town = Console.readLine("Town");
        final String country = Console.readLine("Country");
        final double latitude = Console.readDouble("Latitude (-90 to 90)");
        final double longitude = Console.readDouble("Longitude (-180 to 180)");
        final double elevation = Console.readDouble("Elevation (meters above sea level)");
        final String areaCode = Console.readLine("Air Control Area Code");

        try {
            controller.registerAirport(icaoCode, iataCode, name, town, country,
                    latitude, longitude, elevation, areaCode);
            System.out.println("Airport registered successfully.");
        } catch (final IntegrityViolationException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register Airport";
    }
}
