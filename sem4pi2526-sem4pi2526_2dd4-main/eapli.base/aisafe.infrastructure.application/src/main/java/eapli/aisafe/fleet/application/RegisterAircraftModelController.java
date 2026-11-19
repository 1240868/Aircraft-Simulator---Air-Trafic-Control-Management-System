package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UseCaseController
public class RegisterAircraftModelController {

    private final AuthorizationPort authz;
    private final AircraftModelRepository aircraftRepo;
    private final EngineModelRepository engineRepo;

    public RegisterAircraftModelController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().engineModels());
    }

    public RegisterAircraftModelController(final AuthorizationPort authz,
                                           final AircraftModelRepository aircraftRepo,
                                           final EngineModelRepository engineRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.engineRepo = engineRepo;
    }

    /**
     * Lists existing engine models so the operator can copy IDs into the aircraft registration form.
     */
    public Iterable<EngineModel> listEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);
        return engineRepo.findAll();
    }

    public AircraftModel register(final String modelName,
                                  final String manufacturer,
                                  final AircraftType type,
                                  final int numberOfEngines,
                                  final int passengerCapacity,
                                  final double maximumRangeKm,
                                  final double emptyWeightKg,
                                  final double mtowKg,
                                  final double mzfwKg,
                                  final double maxFuelCapacityLiters,
                                  final double serviceCeilingM,
                                  final double cruiseSpeedMs,
                                  final double wingAreaM2,
                                  final double cd0,
                                  final double cl,
                                  final List<EngineModelId> certifiedEngineModelIds) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);

        final Optional<AircraftModel> existing = aircraftRepo.findByModelNameAndManufacturer(modelName, manufacturer);
        if (existing.isPresent()) {
            throw new IntegrityViolationException("Aircraft model already exists: " + modelName + " / " + manufacturer);
        }

        final List<EngineType> types = new ArrayList<>();
        for (final var engineId : certifiedEngineModelIds) {
            final var engine = engineRepo.ofIdentity(engineId)
                    .orElseThrow(() -> new IntegrityViolationException("Unknown engine model id: " + engineId));
            types.add(engine.type());
        }

        if (types.isEmpty()) {
            throw new IntegrityViolationException("At least one certified engine model is required");
        }

        final EngineType motorization = types.get(0);
        for (final var t : types) {
            if (t != motorization) {
                throw new IntegrityViolationException("All certified engines must have same type (motorization)");
            }
        }

        final var aircraftModel = new AircraftModel(
                modelName,
                manufacturer,
                type,
                motorization,
                numberOfEngines,
                passengerCapacity,
                maximumRangeKm,
                emptyWeightKg,
                mtowKg,
                mzfwKg,
                maxFuelCapacityLiters,
                serviceCeilingM,
                cruiseSpeedMs,
                wingAreaM2,
                cd0,
                cl,
                certifiedEngineModelIds
        );

        return aircraftRepo.save(aircraftModel);
    }
}
