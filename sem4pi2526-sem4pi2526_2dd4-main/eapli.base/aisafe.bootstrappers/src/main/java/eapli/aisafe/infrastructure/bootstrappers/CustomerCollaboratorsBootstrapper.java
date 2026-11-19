package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.aircontrol.application.RegisterAreaCollaboratorDTO;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.company.application.AddCollaboratorController;
import eapli.aisafe.company.application.RegisterCustomerCollaboratorDTO;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.IntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds at least one company collaborator so the behaviour is reproducible from bootstrap.
 */
public class CustomerCollaboratorsBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerCollaboratorsBootstrapper.class);
    private final AddCollaboratorController collaboratorController = new AddCollaboratorController();

    @Override
    public boolean execute() {
        final var companies = PersistenceContext.repositories().airTransportCompanies();
        companies.findByIataCode("TP").ifPresentOrElse(
                tap -> registerCompanyCollabIfAbsent(tap.identity().asUuid().toString(), "tap_collab", "TapCollab1!",
                        "collaborator@tap.pt", "TAP", "Collaborator", "+351 211 000 000"),
                () -> LOGGER.warn("No company with IATA TP; skipping company collaborator bootstrap"));

        final var areas = PersistenceContext.repositories().airControlAreas();
        areas.findByCode(AirControlAreaCode.valueOf("PT-LIS")).ifPresentOrElse(
                lis -> {
                    registerAreaCollabIfAbsent(lis.identity().toString(), "lis_collab", "LisCollab1!",
                            "collaborator@fir.pt", "LIS", "Collaborator", "+351 213 000 000");
                    linkFcoToArea(lis, areas);
                },
                () -> LOGGER.warn("No air control area PT-LIS; skipping area collaborator bootstrap"));
        return true;
    }

    private void registerCompanyCollabIfAbsent(final String companyId, final String username, final String password,
                                               final String email, final String firstName, final String lastName,
                                               final String phone) {
        try {
            collaboratorController.registerCollaboratorForAirTransportCompany(
                    new RegisterCustomerCollaboratorDTO(companyId, username, password, email,
                            firstName, lastName, phone));
            LOGGER.debug("Registered company collaborator {}", username);
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming collaborator already exists: {} ({})", username, ex.getMessage());
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            LOGGER.warn("Skipping collaborator {}: {}", username, ex.getMessage());
        }
    }

    private void registerAreaCollabIfAbsent(final String areaId, final String username, final String password,
                                            final String email, final String firstName, final String lastName,
                                            final String phone) {
        try {
            collaboratorController.registerCollaboratorForAirControlArea(
                    new RegisterAreaCollaboratorDTO(areaId, username, password, email,
                            firstName, lastName, phone));
            LOGGER.debug("Registered area collaborator {}", username);
        } catch (final IntegrityViolationException ex) {
            LOGGER.warn("Assuming area collaborator already exists: {} ({})", username, ex.getMessage());
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            LOGGER.warn("Skipping area collaborator {}: {}", username, ex.getMessage());
        }
    }

    private void linkFcoToArea(final AirControlArea area, final AirControlAreaRepository repo) {
        if (area.collaboratorUsernames().contains("fco1")) {
            return;
        }
        area.addCollaborator("fco1");
        repo.save(area);
        LOGGER.debug("Linked fco1 to area {}", area.identity());
    }
}
