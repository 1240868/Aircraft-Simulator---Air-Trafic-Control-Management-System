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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@UseCaseController
public class ListFleetByAgeController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final Supplier<LocalDate> today;

    public ListFleetByAgeController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies(),
                LocalDate::now);
    }

    public ListFleetByAgeController(final AuthorizationPort authz,
                                    final AircraftRepository aircraftRepo,
                                    final AircraftModelRepository modelRepo,
                                    final AirTransportCompanyRepository companyRepo,
                                    final Supplier<LocalDate> today) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
        this.today = today;
    }

    /** Lists active aircraft whose age in complete years equals the given value (US072d). */
    public FleetListResult listFleetByAge(final String ageYearsRaw) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, authz.currentUsername());
        final int ageYears = parsePositiveAgeYears(ageYearsRaw);
        final LocalDate asOf = today.get();

        final List<AircraftFleetListItemDTO> rows = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findActiveByOwnerCompanyAndAge(companyId, ageYears, asOf)) {
            rows.add(FleetListing.toRowWithAge(a, FleetListing.requireModelForAircraft(modelRepo, a), asOf));
        }
        rows.sort(Comparator.comparing(AircraftFleetListItemDTO::registrationNumber));
        return FleetListResult.of(rows, FleetListResult.NO_RESULTS_BY_AGE);
    }

    private static int parsePositiveAgeYears(final String raw) {
        Preconditions.nonEmpty(raw, "Age is required");
        final String s = raw.strip();
        Preconditions.ensure(!s.isEmpty(), "Age is required");
        try {
            final int v = Integer.parseInt(s);
            Preconditions.ensure(v > 0, "Age must be a positive integer");
            return v;
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid age: " + s, ex);
        }
    }
}
