package eapli.aisafe.app.user.console.presentation.fleet;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.AddAircraftController;
import eapli.aisafe.fleet.application.AircraftModelOptionDTO;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class AddAircraftUI extends AbstractBaseUI {

    private final AddAircraftController controller = new AddAircraftController();

    private static LocalDate readOptionalIsoDate(final String prompt) {
        final String s = Console.readLine(prompt).strip();
        if (s.isEmpty()) {
            return null;
        }
        return LocalDate.parse(s);
    }

    @Override
    protected boolean doShow() {
        final List<AircraftModelOptionDTO> models = controller.listAircraftModels();
        if (models.isEmpty()) {
            System.out.println("No aircraft models available. Please register a model first.");
            return false;
        }

        System.out.println("\n--- Available Aircraft Models ---");
        int idx = 1;
        for (final AircraftModelOptionDTO dto : models) {
            System.out.printf("  %d. %s / %s  [%s]  capacity=%d  (id=%s)%n",
                    idx++, dto.modelName(), dto.manufacturer(), dto.type(),
                    dto.passengerCapacity(), dto.modelId());
        }
        System.out.println("---------------------------------");

        final int choice = Console.readInteger("Select model number");
        if (choice < 1 || choice > models.size()) {
            System.out.println("Invalid selection.");
            return false;
        }
        final AircraftModelOptionDTO selectedModel = models.get(choice - 1);

        final String registration = Console.readLine("Aircraft registration number (tail number, e.g. CS-TUA)");
        final String country = Console.readLine("Registration country (ISO 3166-1 alpha-2, e.g. PT)");

        System.out.println("\n--- Age (at least one date, yyyy-MM-dd) ---");
        System.out.println("Manufacturing date is used for fleet age when set; otherwise first registration.");
        final LocalDate manufacturingDate;
        final LocalDate firstRegistrationDate;
        try {
            manufacturingDate = readOptionalIsoDate("Manufacturing date (yyyy-MM-dd, empty to skip): ");
            firstRegistrationDate = readOptionalIsoDate("First registration date (yyyy-MM-dd, empty to skip): ");
        } catch (final DateTimeParseException ex) {
            System.out.println("Invalid date format. Use yyyy-MM-dd.");
            return false;
        }

        System.out.println("\n--- Cabin seat configuration (max " + selectedModel.passengerCapacity() + " seats) ---");
        final List<CabinSeatBlock> cabinBlocks = new ArrayList<>();
        boolean addMore = true;
        int totalSoFar = 0;
        while (addMore) {
            final String cabinClass = Console.readLine("Cabin class name (e.g. ECONOMY, BUSINESS, FIRST)");
            final int seats = Console.readInteger("Number of seats for " + cabinClass);
            cabinBlocks.add(new CabinSeatBlock(cabinClass.trim(), seats));
            totalSoFar += seats;
            System.out.println("  Total configured so far: " + totalSoFar + " / " + selectedModel.passengerCapacity());
            final String more = Console.readLine("Add another cabin class? (y/n)");
            addMore = more.strip().equalsIgnoreCase("y");
        }

        try {
            final Aircraft aircraft = controller.addAircraft(registration, country,
                    selectedModel.modelId(), cabinBlocks, manufacturingDate, firstRegistrationDate);
            System.out.println("\nAircraft registered successfully!");
            System.out.println("  Registration: " + aircraft.identity());
            System.out.println("  Model: " + selectedModel.modelName() + " / " + selectedModel.manufacturer());
            System.out.println("  Country: " + aircraft.registrationCountry());
            System.out.println("  Status: " + aircraft.operationalStatus());
            System.out.println("  Total seats: " + aircraft.totalConfiguredSeats());
            aircraft.manufacturingDate().ifPresent(d -> System.out.println("  Manufacturing date: " + d));
            aircraft.firstRegistrationDate().ifPresent(d -> System.out.println("  First registration date: " + d));
        } catch (final IntegrityViolationException | IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Could not register aircraft: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Add aircraft to fleet";
    }
}
