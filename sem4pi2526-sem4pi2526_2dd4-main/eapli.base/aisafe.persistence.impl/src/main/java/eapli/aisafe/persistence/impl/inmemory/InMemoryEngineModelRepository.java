package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.fleet.domain.EngineModel;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, EngineModelId>
        implements EngineModelRepository {

    @Override
    public Optional<EngineModel> findByModelNameAndManufacturer(final String modelName, final String manufacturer) {
        for (final var em : super.findAll()) {
            if (em.modelName().equalsIgnoreCase(modelName)
                    && em.manufacturer().equalsIgnoreCase(manufacturer)) {
                return Optional.of(em);
            }
        }
        return Optional.empty();
    }
}

