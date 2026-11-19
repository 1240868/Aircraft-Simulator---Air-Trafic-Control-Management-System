package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.aisafe.fleet.domain.ElectricEngineSpecs;
import eapli.aisafe.fleet.domain.FuelSpecs;
import eapli.aisafe.fleet.domain.FuelType;
import eapli.aisafe.fleet.domain.JetEngineSpecs;
import eapli.aisafe.fleet.domain.Power;
import eapli.aisafe.fleet.domain.Thrust;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.util.Optional;

@UseCaseController
public class RegisterEngineModelController {

    private final AuthorizationPort authz;
    private final EngineModelRepository repo;

    public RegisterEngineModelController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().engineModels());
    }

    public RegisterEngineModelController(final AuthorizationPort authz, final EngineModelRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    public EngineModel registerJet(final String modelName, final String manufacturer,
                                   final EngineType type, final double seaLevelStaticThrustValue,
                                   final String thrustUnit, final double cruiseThrustValue,
                                   final double tsfc, final double fuelDensity,
                                   final double fuelSpecificEnergy) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);
        final Optional<EngineModel> existing = repo.findByModelNameAndManufacturer(modelName, manufacturer);
        if (existing.isPresent()) {
            throw new IntegrityViolationException("Engine model already exists: " + modelName + " / " + manufacturer);
        }
        if (type == EngineType.ELECTRIC_PROPELLER) {
            throw new IntegrityViolationException("Use registerElectric for electric engines");
        }
        final var fuel = new FuelSpecs(FuelType.JET_A1, fuelDensity, fuelSpecificEnergy);
        final var specs = new JetEngineSpecs(
                new Thrust(seaLevelStaticThrustValue, thrustUnit),
                new Thrust(cruiseThrustValue, thrustUnit),
                tsfc
        );
        final var engineModel = EngineModel.createJet(modelName, manufacturer, type, fuel, specs);
        return repo.save(engineModel);
    }

    public EngineModel registerElectric(final String modelName, final String manufacturer,
                                        final double powerValue, final String powerUnit,
                                        final double efficiency, final double energyDensity,
                                        final double specificEnergy) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);
        final Optional<EngineModel> existing = repo.findByModelNameAndManufacturer(modelName, manufacturer);
        if (existing.isPresent()) {
            throw new IntegrityViolationException("Engine model already exists: " + modelName + " / " + manufacturer);
        }
        final var fuel = new FuelSpecs(FuelType.ELECTRICITY, energyDensity, specificEnergy);
        final var specs = new ElectricEngineSpecs(new Power(powerValue, powerUnit), efficiency);
        final var engineModel = EngineModel.createElectric(modelName, manufacturer, fuel, specs);
        return repo.save(engineModel);
    }
}
