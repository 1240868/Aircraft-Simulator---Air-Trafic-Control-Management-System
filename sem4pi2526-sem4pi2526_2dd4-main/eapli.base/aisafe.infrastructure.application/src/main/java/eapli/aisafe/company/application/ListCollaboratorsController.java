package eapli.aisafe.company.application;

import eapli.aisafe.aircontrol.application.AirControlAreaOptionDTO;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@UseCaseController
public class ListCollaboratorsController {

    private final AuthorizationService authz;
    private final UserManagementService userSvc;
    private final AirTransportCompanyRepository companyRepo;
    private final AirControlAreaRepository areaRepo;

    public ListCollaboratorsController() {
        this(AuthzRegistry.authorizationService(),
                AuthzRegistry.userService(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().airControlAreas());
    }

    public ListCollaboratorsController(final AuthorizationService authz,
                                       final UserManagementService userSvc,
                                       final AirTransportCompanyRepository companyRepo,
                                       final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.userSvc = userSvc;
        this.companyRepo = companyRepo;
        this.areaRepo = areaRepo;
    }

    public Iterable<AirTransportCompanyOptionDTO> listAirTransportCompaniesForSelection() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);
        final AirTransportCompanyRepository companies = companyRepo;
        final List<AirTransportCompanyOptionDTO> out = new ArrayList<>();
        for (final AirTransportCompany c : companies.findAll()) {
            out.add(new AirTransportCompanyOptionDTO(
                    c.identity().asUuid().toString(),
                    c.name().value(),
                    c.iataCode().value()));
        }
        return out;
    }

    public Iterable<AirControlAreaOptionDTO> listAirControlAreasForSelection() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);
        final AirControlAreaRepository areas = areaRepo;
        final List<AirControlAreaOptionDTO> out = new ArrayList<>();
        for (final AirControlArea a : areas.findAll()) {
            out.add(new AirControlAreaOptionDTO(
                    a.identity().toString(),
                    a.name(),
                    a.areaCode().toString()));
        }
        return out;
    }

    public List<CollaboratorDTO> listActiveCollaboratorsForCompany(final String companyId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);
        final CompanyId id = new CompanyId(parseUuid(companyId, "company id"));
        final AirTransportCompany company = companyRepo.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("Air transport company not found: " + companyId));
        return listActiveUsersByUsernames(company.collaboratorUsernames());
    }

    public List<CollaboratorDTO> listActiveCollaboratorsForAirControlArea(final String areaId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);
        final AirControlAreaCode id = AirControlAreaCode.valueOf(areaId.strip());
        final AirControlArea area = areaRepo.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("Air control area not found: " + areaId));
        return listActiveUsersByUsernames(area.collaboratorUsernames());
    }

    private UUID parseUuid(final String raw, final String label) {
        try {
            return UUID.fromString(raw.strip());
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + label, ex);
        }
    }

    private List<CollaboratorDTO> listActiveUsersByUsernames(final Set<String> usernames) {
        final Set<String> wanted = new HashSet<>();
        for (final String u : usernames) {
            if (u != null && !u.isBlank()) {
                wanted.add(u.strip());
            }
        }
        final List<CollaboratorDTO> out = new ArrayList<>();
        for (final SystemUser u : userSvc.activeUsers()) {
            final String username = u.username().toString();
            if (wanted.contains(username)) {
                out.add(new CollaboratorDTO(
                        username,
                        u.name().firstName(),
                        u.name().lastName(),
                        u.email() == null ? null : u.email().toString(),
                        null));
            }
        }
        return out;
    }
}

