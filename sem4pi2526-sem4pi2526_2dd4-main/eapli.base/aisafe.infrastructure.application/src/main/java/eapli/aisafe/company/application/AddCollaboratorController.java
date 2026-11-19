package eapli.aisafe.company.application;

import eapli.aisafe.aircontrol.application.AirControlAreaOptionDTO;
import eapli.aisafe.aircontrol.application.RegisterAreaCollaboratorDTO;
import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.usermanagement.domain.CollaboratorUserFactory;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;
import java.util.Set;
import java.util.UUID;

@UseCaseController
public class AddCollaboratorController {

    private final AuthorizationService authz;
    private final UserManagementService userSvc;
    private final ListCollaboratorsController customerCatalog;
    private final AirTransportCompanyRepository companyRepo;
    private final AirControlAreaRepository areaRepo;

    public AddCollaboratorController() {
        this(AuthzRegistry.authorizationService(),
                AuthzRegistry.userService(),
                new ListCollaboratorsController(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().airControlAreas());
    }

    public AddCollaboratorController(final AuthorizationService authz,
                                     final UserManagementService userSvc,
                                     final ListCollaboratorsController customerCatalog,
                                     final AirTransportCompanyRepository companyRepo,
                                     final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.userSvc = userSvc;
        this.customerCatalog = customerCatalog;
        this.companyRepo = companyRepo;
        this.areaRepo = areaRepo;
    }

    public Iterable<AirTransportCompanyOptionDTO> listAirTransportCompaniesForSelection() {
        return customerCatalog.listAirTransportCompaniesForSelection();
    }

    public Iterable<AirControlAreaOptionDTO> listAirControlAreasForSelection() {
        return customerCatalog.listAirControlAreasForSelection();
    }

    /**
     * Registers a new system user with the company collaborator role and links the username to the company.
     */
    public SystemUser registerCollaboratorForAirTransportCompany(final RegisterCustomerCollaboratorDTO dto) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);

        CollaboratorUserFactory.newCollaboratorUser(
                dto.email(), dto.firstName(), dto.lastName(), dto.phone());

        final UUID companyUuid;
        try {
            companyUuid = UUID.fromString(dto.companyId().strip());
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid company id", ex);
        }
        final CompanyId companyId = new CompanyId(companyUuid);

        final AirTransportCompany company = companyRepo.ofIdentity(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Air transport company not found: " + dto.companyId()));

        final String normalizedEmail = dto.email().strip().toLowerCase();
        if (emailAlreadyRegistered(normalizedEmail)) {
            throw new IntegrityViolationException("Email already registered: " + dto.email());
        }

        final Set<Role> roles = Set.of(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final SystemUser created = userSvc.registerNewUser(
                dto.username(),
                dto.password(),
                dto.firstName().strip(),
                dto.lastName().strip(),
                dto.email().strip(),
                roles,
                CurrentTimeCalendars.now());

        company.addCollaborator(created.username().toString(), dto.phone());
        companyRepo.save(company);
        return created;
    }

    /**
     * Registers a new system user with the air control area collaborator role and links the username to the area.
     */
    public SystemUser registerCollaboratorForAirControlArea(final RegisterAreaCollaboratorDTO dto) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);

        CollaboratorUserFactory.newCollaboratorUser(
                dto.email(), dto.firstName(), dto.lastName(), dto.phone());

        final AirControlAreaCode areaCode = AirControlAreaCode.valueOf(dto.areaId().strip());

        final AirControlArea area = areaRepo.ofIdentity(areaCode)
                .orElseThrow(() -> new IllegalArgumentException("Air control area not found: " + dto.areaId()));

        final String normalizedEmail = dto.email().strip().toLowerCase();
        if (emailAlreadyRegistered(normalizedEmail)) {
            throw new IntegrityViolationException("Email already registered: " + dto.email());
        }

        final Set<Role> roles = Set.of(Roles.AIR_CONTROL_AREA_COLLABORATOR);
        final SystemUser created = userSvc.registerNewUser(
                dto.username(),
                dto.password(),
                dto.firstName().strip(),
                dto.lastName().strip(),
                dto.email().strip(),
                roles,
                CurrentTimeCalendars.now());

        area.addCollaborator(created.username().toString(), dto.phone());
        areaRepo.save(area);
        return created;
    }

    private boolean emailAlreadyRegistered(final String normalizedEmail) {
        for (final SystemUser u : userSvc.allUsers()) {
            if (normalizedEmail.equalsIgnoreCase(emailOf(u))) {
                return true;
            }
        }
        return false;
    }

    private static String emailOf(final SystemUser user) {
        if (user.email() == null) {
            return "";
        }
        return user.email().toString().strip().toLowerCase();
    }
}
