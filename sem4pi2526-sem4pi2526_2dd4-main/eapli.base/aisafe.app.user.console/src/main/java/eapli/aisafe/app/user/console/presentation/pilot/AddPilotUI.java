package eapli.aisafe.app.user.console.presentation.pilot;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.fleet.application.AircraftModelOptionDTO;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.application.AddPilotController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddPilotUI extends AbstractBaseUI {

    private final AddPilotController controller = new AddPilotController();

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
            System.out.printf("  %d. %s / %s  [%s]  (id=%s)%n",
                    idx++, dto.modelName(), dto.manufacturer(), dto.type(), dto.modelId());
        }
        System.out.println("---------------------------------");

        final List<AircraftModelId> certifiedModels = parseModelSelections(models);
        if (certifiedModels == null) {
            return false;
        }

        final String username = Console.readLine("Username (for system login)");
        final String password = Console.readLine("Password");
        final String firstName = Console.readLine("First name");
        final String lastName = Console.readLine("Last name");
        final String email = Console.readLine("Email");
        final String certification = Console.readLine("Certification (e.g. ATPL, CPL)");
        final String licenseNumber = Console.readLine("License number");

        final LocalDateTime licenseExpiration;
        try {
            final String rawDate = Console.readLine("License expiration date (yyyy-MM-ddTHH:mm, e.g. 2027-12-31T00:00)");
            licenseExpiration = LocalDateTime.parse(rawDate.strip());
        } catch (final DateTimeParseException ex) {
            System.out.println("Invalid date format. Use yyyy-MM-ddTHH:mm (e.g. 2027-12-31T00:00).");
            return false;
        }

        try {
            final Pilot pilot = controller.addPilot(
                    username, password, firstName, lastName, email,
                    certification, licenseNumber, licenseExpiration, certifiedModels);
            System.out.println("\nPilot added successfully!");
            System.out.println("  Name    : " + pilot.getName().firstName() + " " + pilot.getName().lastName());
            System.out.println("  Email   : " + pilot.getEmail().value());
            System.out.println("  License : " + pilot.getLicenseVoo().value()
                    + " (expires " + pilot.getLicenseVoo().expirationDate() + ")");
            System.out.println("  Models  : " + pilot.getCertifiedModels().size() + " model(s) certified");
            System.out.println("  Status  : " + pilot.getStatus());
        } catch (final IntegrityViolationException | IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Could not add pilot: " + ex.getMessage());
        }
        return false;
    }

    private List<AircraftModelId> parseModelSelections(final List<AircraftModelOptionDTO> models) {
        final String input = Console.readLine(
                "Select certified model numbers (comma-separated, e.g. 1,3)");
        final List<AircraftModelId> result = new ArrayList<>();
        for (final String part : input.split(",")) {
            final String token = part.strip();
            if (token.isEmpty()) {
                continue;
            }
            final int num;
            try {
                num = Integer.parseInt(token);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid selection: " + token);
                return null;
            }
            if (num < 1 || num > models.size()) {
                System.out.println("Selection out of range: " + num);
                return null;
            }
            final AircraftModelId id = new AircraftModelId(UUID.fromString(models.get(num - 1).modelId()));
            if (!result.contains(id)) {
                result.add(id);
            }
        }
        if (result.isEmpty()) {
            System.out.println("At least one aircraft model must be selected.");
            return null;
        }
        return result;
    }

    @Override
    public String headline() {
        return super.headline() + " > Add pilot to company";
    }
}
