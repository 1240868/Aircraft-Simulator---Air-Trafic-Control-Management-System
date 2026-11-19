package eapli.aisafe.app.backoffice.console.presentation.company;

import eapli.aisafe.aircontrol.application.AirControlAreaOptionDTO;
import eapli.aisafe.company.application.AirTransportCompanyOptionDTO;
import eapli.aisafe.company.application.CollaboratorDTO;
import eapli.framework.io.util.Console;
import eapli.framework.util.Utility;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared console prompts for US061 / US062 customer flows.
 */
@Utility
public final class CustomerConsoleHelper {
    private CustomerConsoleHelper() {
    }

    public static final int KIND_COMPANY = 1;
    public static final int KIND_AREA = 2;

    public static Optional<Integer> readCustomerKindOrCancel() {
        final int kind = Console.readInteger(
                "Customer type: 1 = Air transport company, 2 = Air control area, 0 = cancel");
        if (kind == KIND_COMPANY || kind == KIND_AREA) {
            return Optional.of(kind);
        }
        System.out.println("Cancelled.");
        return Optional.empty();
    }

    public static Optional<AirTransportCompanyOptionDTO> pickAirTransportCompany(
            final Iterable<AirTransportCompanyOptionDTO> companies) {
        final List<AirTransportCompanyOptionDTO> list = new ArrayList<>();
        companies.forEach(list::add);
        if (list.isEmpty()) {
            System.out.println("No air transport companies registered yet.");
            return Optional.empty();
        }
        System.out.printf("%-6s%-10s%s%n", "Nº", "IATA", "Company");
        int i = 1;
        for (final AirTransportCompanyOptionDTO c : list) {
            System.out.printf("%-6d%-10s%s%n", i, c.iataCode(), c.displayName());
            i++;
        }
        final int choice = Console.readInteger("Select company nº (0 = cancel)");
        if (choice < 1 || choice > list.size()) {
            System.out.println("Cancelled or invalid selection.");
            return Optional.empty();
        }
        return Optional.of(list.get(choice - 1));
    }

    public static Optional<AirControlAreaOptionDTO> pickAirControlArea(
            final Iterable<AirControlAreaOptionDTO> areas) {
        final List<AirControlAreaOptionDTO> list = new ArrayList<>();
        areas.forEach(list::add);
        if (list.isEmpty()) {
            System.out.println("No air control areas registered yet.");
            return Optional.empty();
        }
        System.out.printf("%-6s%-14s%s%n", "Nº", "Code", "Area");
        int i = 1;
        for (final AirControlAreaOptionDTO a : list) {
            System.out.printf("%-6d%-14s%s%n", i, a.areaCode(), a.displayName());
            i++;
        }
        final int choice = Console.readInteger("Select area nº (0 = cancel)");
        if (choice < 1 || choice > list.size()) {
            System.out.println("Cancelled or invalid selection.");
            return Optional.empty();
        }
        return Optional.of(list.get(choice - 1));
    }

    public static void printActiveCollaborators(final List<CollaboratorDTO> collaborators) {
        if (collaborators.isEmpty()) {
            System.out.println("No active collaborators found for this customer.");
            return;
        }
        System.out.printf("%-16s%-20s%-20s%-30s%n", "Username", "First name", "Last name", "Email");
        for (final CollaboratorDTO c : collaborators) {
            System.out.printf("%-16s%-20s%-20s%-30s%n",
                    c.username(),
                    c.firstName(),
                    c.lastName(),
                    c.email() == null ? "" : c.email());
        }
    }
}
