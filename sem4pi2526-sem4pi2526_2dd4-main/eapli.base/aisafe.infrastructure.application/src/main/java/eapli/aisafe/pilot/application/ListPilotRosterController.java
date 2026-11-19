package eapli.aisafe.pilot.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.application.dto.PilotRosterListItemDTO;
import eapli.aisafe.pilot.application.dto.PilotRosterListResult;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UseCaseController
public class ListPilotRosterController {

    private final AuthorizationPort authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository modelRepo;

    public ListPilotRosterController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    public ListPilotRosterController(final AuthorizationPort authz,
                                     final PilotRepository pilotRepo,
                                     final AirTransportCompanyRepository companyRepo,
                                     final AircraftModelRepository modelRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
        this.modelRepo = modelRepo;
    }

    /** Lists active pilots for the authenticated collaborator's company. */
    public PilotRosterListResult listPilotRoster() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final String username = authz.currentUsername();
        final CompanyId companyId = PilotRosterListing.requireCompanyOfUser(companyRepo, username);

        final List<PilotRosterListItemDTO> rows = new ArrayList<>();
        for (final Pilot pilot : pilotRepo.findActiveByCompanyId(companyId)) {
            rows.add(PilotRosterListing.toRow(pilot, modelRepo));
        }
        rows.sort(Comparator.comparing(PilotRosterListItemDTO::fullName));

        final String messageWhenEmpty = rows.isEmpty()
                ? PilotRosterListResult.NO_ACTIVE_PILOTS
                : null;

        return new PilotRosterListResult(List.copyOf(rows), messageWhenEmpty);
    }
}
