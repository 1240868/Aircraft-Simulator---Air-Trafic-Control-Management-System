package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@UseCaseController
public class ListFleetByModelController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;

    public ListFleetByModelController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public ListFleetByModelController(final AuthorizationPort authz,
                                      final AircraftRepository aircraftRepo,
                                      final AircraftModelRepository modelRepo,
                                      final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
    }

    /**
     * Lists active aircraft of the given model for the authenticated collaborator's company (US072a).
     */
    public FleetListResult listFleetByModel(final String aircraftModelIdRaw) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, authz.currentUsername());
        final AircraftModelId modelId = parseModelId(aircraftModelIdRaw);
        final AircraftModel model = FleetListing.requireModel(modelRepo, modelId);

        final List<AircraftFleetListItemDTO> rows = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findActiveByOwnerCompanyAndModel(companyId, modelId)) {
            rows.add(FleetListing.toRow(a, model));
        }
        rows.sort(Comparator.comparing(AircraftFleetListItemDTO::registrationNumber));
        return FleetListResult.of(rows, FleetListResult.NO_RESULTS_BY_MODEL);
    }

    private static AircraftModelId parseModelId(final String raw) {
        try {
            return new AircraftModelId(UUID.fromString(raw.strip()));
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid aircraft model id: " + raw, ex);
        }
    }
}
