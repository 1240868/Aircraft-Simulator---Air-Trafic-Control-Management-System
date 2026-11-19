package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;

@UseCaseController
public class DecommissionAircraftController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final PendingFlightsChecker pendingFlights;

    public DecommissionAircraftController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().pendingFlightsChecker());
    }

    public DecommissionAircraftController(final AuthorizationPort authz,
                                          final AircraftRepository aircraftRepo,
                                          final AirTransportCompanyRepository companyRepo,
                                          final PendingFlightsChecker pendingFlights) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.companyRepo = companyRepo;
        this.pendingFlights = pendingFlights;
    }

    /** Retires an aircraft from the fleet of the collaborator's company (US071). */
    public void decommission(final String registrationRaw) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final CompanyId companyId = FleetListing.requireCompanyOfUser(companyRepo, authz.currentUsername());

        final RegistrationNumber registration = RegistrationNumber.valueOf(registrationRaw);
        final Aircraft aircraft = aircraftRepo.findByRegistration(registration)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + registration));

        if (!aircraft.ownerCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("That aircraft does not belong to your company");
        }

        if (pendingFlights.hasPendingFlights(registration)) {
            throw new IllegalStateException(
                    "Cannot retire an aircraft that has pending flights registered in the system");
        }

        aircraft.markRetired();
        aircraftRepo.save(aircraft);
    }
}
