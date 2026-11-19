package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class JpaEngineModelRepository
        extends BaseJpaRepositoryBase<EngineModel, EngineModelId, EngineModelId>
        implements EngineModelRepository {

    JpaEngineModelRepository() {
        super("id");
    }

    @Override
    public Optional<EngineModel> findByModelNameAndManufacturer(final String modelName,
                                                                final String manufacturer) {
        final Map<String, Object> params = new HashMap<>();
        params.put("modelName", modelName);
        params.put("manufacturer", manufacturer);
        return matchOne(
                "LOWER(e.modelName) = LOWER(:modelName) AND LOWER(e.manufacturer) = LOWER(:manufacturer)",
                params);
    }
}
