package eapli.aisafe.app.backoffice.console.presentation.company;

import eapli.aisafe.company.application.RegisterAirTransportCompanyController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import java.time.LocalDate;

public class RegisterAirTransportCompanyUI extends AbstractUI {

    private final RegisterAirTransportCompanyController controller = new RegisterAirTransportCompanyController();

    @Override
    protected boolean doShow() {
        final String name = Console.readLine("Company name");
        final String iata = Console.readLine("IATA code (2 letters)");
        final String icao = Console.readLine("ICAO code (2 or 3 letters)");
        final String certNumber = Console.readLine("Air operator certificate number");
        final String expiryStr = Console.readLine("Certificate expiry (yyyy-MM-dd)");

        try {
            final LocalDate expiry = LocalDate.parse(expiryStr.trim());
            final var company = controller.register(name, iata, icao, certNumber, expiry);
            System.out.println("Registered: " + company.name().value()
                    + " IATA=" + company.iataCode().value()
                    + " ICAO=" + company.icaoCode().value()
                    + " id=" + company.identity());
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final Exception ex) {
            System.out.println("Invalid input: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register air transport company";
    }
}
