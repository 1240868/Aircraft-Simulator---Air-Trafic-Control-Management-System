package eapli.aisafe.pilot.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;

@UseCaseController
public class DeactivatePilotController {

    private final AuthorizationPort authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final FlightPlanRepository flightPlanRepo;

    public DeactivatePilotController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().flightPlans());
    }

    public DeactivatePilotController(final AuthorizationPort authz,
                                     final PilotRepository pilotRepo,
                                     final AirTransportCompanyRepository companyRepo,
                                     final FlightPlanRepository flightPlanRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
        this.flightPlanRepo = flightPlanRepo;
    }

    /** Deactivates a pilot from the authenticated ATCC's company roster. */
    public void deactivatePilot(final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final String username = authz.currentUsername();
        final CompanyId companyId = PilotRosterListing.requireCompanyOfUser(companyRepo, username);

        final Pilot pilot = pilotRepo.ofIdentity(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found"));

        if (!pilot.getCompany().equals(companyId)) {
            throw new IllegalArgumentException("Pilot does not belong to the authenticated company");
        }

        final boolean hasPlans = flightPlanRepo.hasAssignedFlightPlansFor(pilotId);
        pilot.deactivate(hasPlans);
        pilotRepo.save(pilot);
    }
}
