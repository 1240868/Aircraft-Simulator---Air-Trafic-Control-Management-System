package eapli.aisafe.pilot.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.application.AircraftModelOptionDTO;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.License;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotCertification;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.time.util.CurrentTimeCalendars;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@UseCaseController
public class AddPilotController {

    private final AuthorizationPort authz;
    private final UserManagementService userSvc;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository modelRepo;

    public AddPilotController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                AuthzRegistry.userService(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels());
    }

    public AddPilotController(final AuthorizationPort authz,
                               final UserManagementService userSvc,
                               final PilotRepository pilotRepo,
                               final AirTransportCompanyRepository companyRepo,
                               final AircraftModelRepository modelRepo) {
        this.authz = authz;
        this.userSvc = userSvc;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
        this.modelRepo = modelRepo;
    }

    public List<AircraftModelOptionDTO> listAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final List<AircraftModelOptionDTO> out = new ArrayList<>();
        for (final AircraftModel m : modelRepo.findAll()) {
            out.add(new AircraftModelOptionDTO(
                    m.identity().toString(),
                    m.modelName(),
                    m.manufacturer(),
                    m.type().name(),
                    m.passengerCapacity()));
        }
        return out;
    }

    public CompanyId resolveCompanyOfAuthenticatedUser() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final String username = authz.currentUsername();
        return companyRepo.findCompanyIdForCollaboratorUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user is not linked to any air transport company"));
    }

    public Pilot addPilot(final String username,
                          final String password,
                          final String firstName,
                          final String lastName,
                          final String email,
                          final String certificationValue,
                          final String licenseNumber,
                          final LocalDateTime licenseExpiration,
                          final List<AircraftModelId> certifiedModels) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        if (certifiedModels == null || certifiedModels.isEmpty()) {
            throw new IllegalArgumentException("At least one certified aircraft model is required");
        }

        for (final AircraftModelId modelId : certifiedModels) {
            modelRepo.ofIdentity(modelId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aircraft model not found: " + modelId));
        }

        final Email pilotEmail = new Email(email);
        if (pilotRepo.findByEmail(pilotEmail).isPresent()) {
            throw new IntegrityViolationException("A pilot with email " + email + " already exists");
        }

        final CompanyId companyId = resolveCompanyOfAuthenticatedUser();
        final FullName name = new FullName(firstName, lastName);
        final PilotCertification certification = new PilotCertification(certificationValue);
        final License license = new License(licenseNumber, licenseExpiration);

        final Set<Role> roles = Set.of(Roles.PILOT);
        userSvc.registerNewUser(username, password, firstName.strip(),
                lastName.strip(), email.strip(), roles, CurrentTimeCalendars.now());

        final Pilot pilot = new Pilot(PilotId.newId(), name, pilotEmail, companyId,
                certification, license, certifiedModels);
        return pilotRepo.save(pilot);
    }
}
