package eapli.aisafe.app.backoffice.console.presentation.company;

import eapli.aisafe.aircontrol.application.RegisterAreaCollaboratorDTO;
import eapli.aisafe.company.application.AddCollaboratorController;
import eapli.aisafe.company.application.RegisterCustomerCollaboratorDTO;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class AddCollaboratorUI extends AbstractUI {

    private final AddCollaboratorController controller = new AddCollaboratorController();

    @Override
    protected boolean doShow() {
        final var kind = CustomerConsoleHelper.readCustomerKindOrCancel();
        if (kind.isEmpty()) {
            return false;
        }
        if (kind.get() == CustomerConsoleHelper.KIND_COMPANY) {
            return registerForCompany();
        }
        return registerForArea();
    }

    private boolean registerForCompany() {
        final var selected = CustomerConsoleHelper.pickAirTransportCompany(
                controller.listAirTransportCompaniesForSelection());
        return selected.map(s -> readCredentialsAndRegisterCompany(s.companyId())).orElse(false);
    }

    private boolean registerForArea() {
        final var selected = CustomerConsoleHelper.pickAirControlArea(
                controller.listAirControlAreasForSelection());
        return selected.map(s -> readCredentialsAndRegisterArea(s.areaId())).orElse(false);
    }

    private boolean readCredentialsAndRegisterCompany(final String companyId) {
        final String username = Console.readLine("Username");
        final String password = Console.readLine("Password");
        final String email = Console.readLine("Email");
        final String firstName = Console.readLine("First name");
        final String lastName = Console.readLine("Last name");
        final String phone = Console.readLine("Phone");

        try {
            final var dto = new RegisterCustomerCollaboratorDTO(
                    companyId, username, password, email, firstName, lastName, phone);
            final var user = controller.registerCollaboratorForAirTransportCompany(dto);
            System.out.println("Registered collaborator: @" + user.username());
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        }
        return false;
    }

    private boolean readCredentialsAndRegisterArea(final String areaId) {
        final String username = Console.readLine("Username");
        final String password = Console.readLine("Password");
        final String email = Console.readLine("Email");
        final String firstName = Console.readLine("First name");
        final String lastName = Console.readLine("Last name");
        final String phone = Console.readLine("Phone");

        try {
            final var dto = new RegisterAreaCollaboratorDTO(
                    areaId, username, password, email, firstName, lastName, phone);
            final var user = controller.registerCollaboratorForAirControlArea(dto);
            System.out.println("Registered collaborator: @" + user.username());
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register customer collaborator";
    }
}
