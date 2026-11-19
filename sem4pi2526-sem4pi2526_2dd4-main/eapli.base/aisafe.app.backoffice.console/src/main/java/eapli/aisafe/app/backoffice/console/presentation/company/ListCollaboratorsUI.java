package eapli.aisafe.app.backoffice.console.presentation.company;

import eapli.aisafe.company.application.CollaboratorDTO;
import eapli.aisafe.company.application.ListCollaboratorsController;
import eapli.framework.presentation.console.AbstractUI;
import java.util.List;

@SuppressWarnings("squid:S106")
public class ListCollaboratorsUI extends AbstractUI {

    private final ListCollaboratorsController controller = new ListCollaboratorsController();

    @Override
    protected boolean doShow() {
        final var kind = CustomerConsoleHelper.readCustomerKindOrCancel();
        if (kind.isEmpty()) {
            return false;
        }
        if (kind.get() == CustomerConsoleHelper.KIND_COMPANY) {
            return listForCompany();
        }
        return listForArea();
    }

    private boolean listForCompany() {
        final var selected = CustomerConsoleHelper.pickAirTransportCompany(
                controller.listAirTransportCompaniesForSelection());
        if (selected.isEmpty()) {
            return false;
        }
        final List<CollaboratorDTO> collaborators =
                controller.listActiveCollaboratorsForCompany(selected.get().companyId());
        CustomerConsoleHelper.printActiveCollaborators(collaborators);
        return false;
    }

    private boolean listForArea() {
        final var selected = CustomerConsoleHelper.pickAirControlArea(
                controller.listAirControlAreasForSelection());
        if (selected.isEmpty()) {
            return false;
        }
        final List<CollaboratorDTO> collaborators =
                controller.listActiveCollaboratorsForAirControlArea(selected.get().areaId());
        CustomerConsoleHelper.printActiveCollaborators(collaborators);
        return false;
    }

    @Override
    public String headline() {
        return "List customer collaborators";
    }
}
