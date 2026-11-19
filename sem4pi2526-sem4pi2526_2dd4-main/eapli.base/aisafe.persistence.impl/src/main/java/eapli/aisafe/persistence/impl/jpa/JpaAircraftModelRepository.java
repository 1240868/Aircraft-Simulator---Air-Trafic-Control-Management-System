package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class JpaAircraftModelRepository
        extends BaseJpaRepositoryBase<AircraftModel, AircraftModelId, AircraftModelId>
        implements AircraftModelRepository {

    JpaAircraftModelRepository() {
        super("id");
    }

    @Override
    public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                  final String manufacturer) {
        final Map<String, Object> params = new HashMap<>();
        params.put("modelName", modelName);
        params.put("manufacturer", manufacturer);
        return matchOne(
                "LOWER(e.modelName) = LOWER(:modelName) AND LOWER(e.manufacturer) = LOWER(:manufacturer)",
                params);
    }

    @Override
    public Optional<AircraftModel> findById(final String id) {
        return matchOne("e.id.value = :id", "id", id);
    }

    @Override
    public Optional<AircraftModel> findById(final AircraftModelId id) {
        return ofIdentity(id);
    }
}
