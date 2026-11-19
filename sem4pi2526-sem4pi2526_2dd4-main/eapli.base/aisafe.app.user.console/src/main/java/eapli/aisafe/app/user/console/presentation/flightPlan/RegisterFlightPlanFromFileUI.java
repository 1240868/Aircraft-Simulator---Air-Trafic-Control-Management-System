package eapli.aisafe.app.user.console.presentation.flightPlan;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.flightPlan.application.RegisterFlightPlanFromFileController;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.pilot.Pilot;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RegisterFlightPlanFromFileUI extends AbstractBaseUI {

    private final RegisterFlightPlanFromFileController controller =
            new RegisterFlightPlanFromFileController();

    @Override
    protected boolean doShow() {
        final String filePath = Console.readLine("Path to flight plan file (.dfdsl)");

        final List<Pilot> pilots = toList(controller.listPilots());
        if (pilots.isEmpty()) {
            System.out.println("No pilots registered. Please add a pilot first.");
            return false;
        }
        System.out.println("\n--- Pilots ---");
        for (int i = 0; i < pilots.size(); i++) {
            final Pilot p = pilots.get(i);
            System.out.printf("  %d. %s %s  (license=%s)%n",
                    i + 1, p.getName().firstName(), p.getName().lastName(),
                    p.getLicenseVoo().value());
        }
        final int pilotChoice = Console.readInteger("Select pilot number");
        if (pilotChoice < 1 || pilotChoice > pilots.size()) {
            System.out.println("Invalid selection.");
            return false;
        }
        final Pilot pilot = pilots.get(pilotChoice - 1);

        final List<FlightRoute> routes = toList(controller.listFlightRoutes());
        if (routes.isEmpty()) {
            System.out.println("No flight routes registered. Please create a route first.");
            return false;
        }
        System.out.println("\n--- Flight Routes ---");
        for (int i = 0; i < routes.size(); i++) {
            final FlightRoute r = routes.get(i);
            System.out.printf("  %d. %s  (id=%s)%n", i + 1, r.getName().value(), r.identity());
        }
        final int routeChoice = Console.readInteger("Select flight route number");
        if (routeChoice < 1 || routeChoice > routes.size()) {
            System.out.println("Invalid selection.");
            return false;
        }
        final FlightRoute route = routes.get(routeChoice - 1);

        final List<Aircraft> aircraftList = toList(controller.listAircraft());
        if (aircraftList.isEmpty()) {
            System.out.println("No aircraft registered. Please add an aircraft first.");
            return false;
        }
        System.out.println("\n--- Aircraft ---");
        for (int i = 0; i < aircraftList.size(); i++) {
            final Aircraft a = aircraftList.get(i);
            System.out.printf("  %d. %s  (model=%s, status=%s)%n",
                    i + 1, a.identity(), a.aircraftModelId(), a.operationalStatus());
        }
        final int aircraftChoice = Console.readInteger("Select aircraft number");
        if (aircraftChoice < 1 || aircraftChoice > aircraftList.size()) {
            System.out.println("Invalid selection.");
            return false;
        }
        final Aircraft aircraft = aircraftList.get(aircraftChoice - 1);

        try {
            final var fp = controller.registerFromFile(
                    Path.of(filePath.trim()),
                    route.identity(),
                    aircraft.identity(),
                    pilot.identity());
            System.out.println("Flight plan registered: " + fp.identity()
                    + "  designator=" + fp.getDesignator().value()
                    + "  status=" + fp.getStatus());
        } catch (final IOException ex) {
            System.out.println("Could not read file: " + ex.getMessage());
        } catch (final IllegalArgumentException | IntegrityViolationException ex) {
            System.out.println("Error: " + ex.getMessage());
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
        return "Register flight plan from DSL file";
    }
}
