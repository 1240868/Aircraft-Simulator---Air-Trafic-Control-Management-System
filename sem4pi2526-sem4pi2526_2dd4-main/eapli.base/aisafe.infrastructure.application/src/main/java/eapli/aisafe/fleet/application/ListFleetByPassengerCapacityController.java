package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
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
public class ListFleetByPassengerCapacityController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;

    public ListFleetByPassengerCapacityController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public ListFleetByPassengerCapacityController(final AuthorizationPort authz,
                                                  final AircraftRepository aircraftRepo,
                                                  final AircraftModelRepository modelRepo,
                                                  final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
    }

    /** Lists active aircraft whose total configured passenger seats equal the given capacity (US072c). */
    public FleetListResult listFleetByPassengerCapacity(final String passengerCapacityRaw) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, authz.currentUsername());
        final int capacity = parsePositivePassengerCapacity(passengerCapacityRaw);

        final List<AircraftFleetListItemDTO> rows = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findActiveByOwnerCompanyAndPassengerCapacity(companyId, capacity)) {
            rows.add(FleetListing.toRow(a, FleetListing.requireModelForAircraft(modelRepo, a)));
        }
        rows.sort(Comparator.comparing(AircraftFleetListItemDTO::registrationNumber));
        return FleetListResult.of(rows, FleetListResult.NO_RESULTS_BY_CAPACITY);
    }

    private static int parsePositivePassengerCapacity(final String raw) {
        Preconditions.nonEmpty(raw, "Passenger capacity is required");
        final String s = raw.strip();
        Preconditions.ensure(!s.isEmpty(), "Passenger capacity is required");
        try {
            final int v = Integer.parseInt(s);
            Preconditions.ensure(v > 0, "Passenger capacity must be a positive integer");
            return v;
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid passenger capacity: " + s, ex);
        }
    }
}
