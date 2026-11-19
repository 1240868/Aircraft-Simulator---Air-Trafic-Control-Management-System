package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.validations.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@UseCaseController
public class AddAircraftController {

    private final AuthorizationPort authz;
    private final AircraftRepository aircraftRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;

    public AddAircraftController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public AddAircraftController(final AuthorizationPort authz,
                                 final AircraftRepository aircraftRepo,
                                 final AircraftModelRepository modelRepo,
                                 final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
    }

    /**
     * Lists all aircraft models as DTOs so the collaborator can select one.
     */
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

    /**
     * Resolves the CompanyId that the currently authenticated collaborator belongs to.
     */
    public CompanyId resolveCompanyOfAuthenticatedUser() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final String username = authz.currentUsername();
        return companyRepo.findCompanyIdForCollaboratorUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user is not linked to any air transport company"));
    }

    /**
     * Registers a new aircraft in the fleet of the authenticated collaborator's company.
     *
     * @param registrationNumber unique tail number (e.g. "CS-TUA")
     * @param countryCode        ISO 3166-1 alpha-2 code (e.g. "PT")
     * @param modelIdStr         UUID string of the chosen AircraftModel
     * @param cabinBlocks        seat allocation per cabin class
     * @param manufacturingDate  date aircraft left factory (optional if firstRegistrationDate is set)
     * @param firstRegistrationDate first civil registration date (optional if manufacturingDate is set)
     * @return the persisted Aircraft
     */
    public Aircraft addAircraft(final String registrationNumber,
                                final String countryCode,
                                final String modelIdStr,
                                final List<CabinSeatBlock> cabinBlocks,
                                final LocalDate manufacturingDate,
                                final LocalDate firstRegistrationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final AircraftModelId modelId = new AircraftModelId(UUID.fromString(modelIdStr.strip()));

        final RegistrationNumber reg = RegistrationNumber.valueOf(registrationNumber);
        if (aircraftRepo.ofIdentity(reg).isPresent()) {
            throw new IntegrityViolationException(
                    "An aircraft with registration " + registrationNumber + " already exists");
        }

        final AircraftModel model = modelRepo.ofIdentity(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + modelIdStr));

        final CompanyId companyId = resolveCompanyOfAuthenticatedUser();

        Preconditions.ensure(manufacturingDate != null || firstRegistrationDate != null,
                "Manufacturing date or first registration date is required");

        final Aircraft aircraft = AircraftFactory.registerNew(
                registrationNumber,
                countryCode,
                modelId,
                companyId,
                new ArrayList<>(cabinBlocks),
                model.passengerCapacity(),
                manufacturingDate,
                firstRegistrationDate);

        return aircraftRepo.save(aircraft);
    }
}
