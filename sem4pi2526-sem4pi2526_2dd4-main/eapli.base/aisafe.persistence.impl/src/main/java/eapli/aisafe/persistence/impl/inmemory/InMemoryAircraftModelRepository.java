package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryAircraftModelRepository
        extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
        implements AircraftModelRepository {

    @Override
    public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName, final String manufacturer) {
        for (final var am : super.findAll()) {
            if (am.modelName().equalsIgnoreCase(modelName)
                    && am.manufacturer().equalsIgnoreCase(manufacturer)) {
                return Optional.of(am);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<AircraftModel> findById(final String id) {
        for (final var m : super.findAll()) {
            if (m.identity().toString().equals(id)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<AircraftModel> findById(final AircraftModelId id) {
        return ofIdentity(id);
    }
}
