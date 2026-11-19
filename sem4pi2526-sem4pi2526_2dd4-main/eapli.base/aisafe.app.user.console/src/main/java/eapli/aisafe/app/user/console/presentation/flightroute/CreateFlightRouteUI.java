package eapli.aisafe.app.user.console.presentation.flightroute;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.flightroute.application.CreateFlightRouteController;
import eapli.aisafe.flightroute.application.FlightRouteDTO;
import eapli.aisafe.flightroute.application.OperationalAirportDTO;
import eapli.framework.io.util.Console;
import java.util.List;

/** US073 — create a flight route for the collaborator's air transport company. */
public class CreateFlightRouteUI extends AbstractBaseUI {

    private final CreateFlightRouteController controller = new CreateFlightRouteController();

    @Override
    protected boolean doShow() {
        final List<OperationalAirportDTO> airports;
        try {
            airports = controller.listOperationalAirports();
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        if (airports.isEmpty()) {
            System.out.println("No operational airports available. Register airports first (US052).");
            return false;
        }

        System.out.println("\n--- Operational airports ---");
        int idx = 1;
        for (final OperationalAirportDTO a : airports) {
            System.out.printf("  %d. %s/%s %s (%s, %s)%n",
                    idx++, a.icaoCode(), a.iataCode(), a.name(), a.town(), a.country());
        }
        System.out.println("----------------------------");

        final int originChoice = Console.readInteger("Select origin airport number");
        final int destChoice = Console.readInteger("Select destination airport number");
        if (originChoice < 1 || originChoice > airports.size()
                || destChoice < 1 || destChoice > airports.size()) {
            System.out.println("Invalid airport selection.");
            return false;
        }

        final OperationalAirportDTO origin = airports.get(originChoice - 1);
        final OperationalAirportDTO destination = airports.get(destChoice - 1);
        if (origin.icaoCode().equals(destination.icaoCode())) {
            System.out.println("Origin and destination must be different airports.");
            return false;
        }

        final String numericSuffix = Console.readLine("Route number suffix (1-4 digits, company prefix is applied automatically): ");
        if (numericSuffix == null || numericSuffix.isBlank()) {
            System.out.println("Route suffix is required.");
            return false;
        }

        try {
            final FlightRouteDTO created = controller.createRoute(
                    numericSuffix.trim(), origin.icaoCode(), destination.icaoCode());
            printCreated(created);
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    private static void printCreated(final FlightRouteDTO created) {
        System.out.println("\n--- Flight route created ---");
        System.out.println("Route:       " + created.routeName());
        System.out.println("Company:     " + created.companyName());
        System.out.println("Origin:      " + created.originAirportIcao() + "/" + created.originAirportIata());
        System.out.println("Destination: " + created.destinationAirportIcao() + "/" + created.destinationAirportIata());
        System.out.printf("Distance:    %.1f km%n", created.distanceKm());
        System.out.println("Duration:    " + created.durationMinutes() + " minutes (estimated)");
        System.out.println("Status:      " + created.status());
        System.out.println("----------------------------");
    }

    @Override
    public String headline() {
        return super.headline() + " > Create flight route";
    }
}
