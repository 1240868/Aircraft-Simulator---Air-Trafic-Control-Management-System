package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.ManufacturerNormalizer;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.validations.Preconditions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UseCaseController
public class ListFleetByMakerController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;

    public ListFleetByMakerController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public ListFleetByMakerController(final AuthorizationPort authz,
                                      final AircraftRepository aircraftRepo,
                                      final AircraftModelRepository modelRepo,
                                      final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
    }

    /** Lists active aircraft whose model manufacturer matches the given maker (US072b). */
    public FleetListResult listFleetByMaker(final String manufacturerRaw) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, authz.currentUsername());

        Preconditions.nonEmpty(manufacturerRaw, "Manufacturer is required");
        final String makerKey = ManufacturerNormalizer.normalize(manufacturerRaw);
        Preconditions.ensure(!makerKey.isEmpty(), "Manufacturer is required");

        if (!FleetListing.isKnownManufacturer(modelRepo, makerKey)) {
            throw new IllegalArgumentException("Unknown manufacturer: " + manufacturerRaw.strip());
        }

        final List<AircraftFleetListItemDTO> rows = new ArrayList<>();
        for (final Aircraft a : FleetListing.activeByManufacturer(aircraftRepo, modelRepo, companyId, makerKey)) {
            final AircraftModel model = FleetListing.requireModelForAircraft(modelRepo, a);
            rows.add(FleetListing.toRow(a, model));
        }
        rows.sort(Comparator.comparing(AircraftFleetListItemDTO::registrationNumber));
        return FleetListResult.of(rows, FleetListResult.NO_RESULTS_BY_MAKER);
    }
}
