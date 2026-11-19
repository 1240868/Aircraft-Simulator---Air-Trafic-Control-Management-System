package eapli.aisafe.fleet.repositories;

import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface EngineModelRepository extends DomainRepository<EngineModelId, EngineModel> {
    Optional<EngineModel> findByModelNameAndManufacturer(String modelName, String manufacturer);
}

