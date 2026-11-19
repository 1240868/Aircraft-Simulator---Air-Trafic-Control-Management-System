package eapli.aisafe.app.user.console.presentation.flightPlan;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.flightPlan.application.RegisterFlightPlanController;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.pilot.Pilot;
import eapli.framework.io.util.Console;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class RegisterFlightPlanUI extends AbstractBaseUI {

    private final RegisterFlightPlanController controller = new RegisterFlightPlanController();

    @Override
    protected boolean doShow() {
        try {
            final List<Pilot> pilotList = toList(controller.listPilots());
            if (pilotList.isEmpty()) {
                System.out.println("No pilots available. Please register a pilot first.");
                return false;
            }

            System.out.println("\n--- Select Pilot ---");
            int idx = 1;
            for (final Pilot p : pilotList) {
                System.out.printf("  %d. %s %s  (license=%s)%n",
                        idx++, p.getName().firstName(), p.getName().lastName(),
                        p.getLicenseVoo().value());
            }
            System.out.println("---------------------");

            final int pilotChoice = Console.readInteger("Select pilot number");
            if (pilotChoice < 1 || pilotChoice > pilotList.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final Pilot selectedPilot = pilotList.get(pilotChoice - 1);

            final List<FlightRoute> routes = toList(controller.listFlightRoutes());
            if (routes.isEmpty()) {
                System.out.println("No flight routes available. Please register a route first.");
                return false;
            }

            System.out.println("\n--- Available Flight Routes ---");
            idx = 1;
            for (final FlightRoute r : routes) {
                System.out.printf("  %d. %s  (id=%s)%n",
                        idx++, r.getName().value(), r.identity());
            }
            System.out.println("-------------------------------");

            final int routeChoice = Console.readInteger("Select flight route number");
            if (routeChoice < 1 || routeChoice > routes.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final FlightRoute selectedRoute = routes.get(routeChoice - 1);

            final List<Aircraft> aircraftList = toList(controller.listAircraft());
            if (aircraftList.isEmpty()) {
                System.out.println("No aircraft available. Please register an aircraft first.");
                return false;
            }

            System.out.println("\n--- Available Aircraft ---");
            idx = 1;
            for (final Aircraft a : aircraftList) {
                System.out.printf("  %d. %s  (model=%s, status=%s)%n",
                        idx++, a.identity(), a.aircraftModelId(), a.operationalStatus());
            }
            System.out.println("--------------------------");

            final int aircraftChoice = Console.readInteger("Select aircraft number");
            if (aircraftChoice < 1 || aircraftChoice > aircraftList.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final Aircraft selectedAircraft = aircraftList.get(aircraftChoice - 1);

            final String designator = Console.readLine("Flight designator (e.g. TP1920): ");
            final String flightType = Console.readLine("Flight type (COMMERCIAL, CARGO, CHARTER, PRIVATE, MILITARY, EMERGENCY): ");
            final String dateTimeStr = Console.readLine("Departure date/time (yyyy-MM-dd HH:mm): ");

            final LocalDateTime departureDateTime;
            try {
                departureDateTime = LocalDateTime.parse(dateTimeStr.strip(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date/time format. Please use yyyy-MM-dd HH:mm.");
                return false;
            }

            controller.register(selectedRoute.identity(), selectedAircraft.identity(),
                    selectedPilot.identity(), designator, flightType, departureDateTime);
            System.out.println("Flight plan registered successfully.");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    private static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    @Override
    public String headline() {
        return super.headline() + " > Register Flight Plan";
    }
}
