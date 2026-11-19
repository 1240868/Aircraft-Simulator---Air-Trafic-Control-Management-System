package eapli.aisafe.fleet.repositories;

import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface AircraftModelRepository extends DomainRepository<AircraftModelId, AircraftModel> {
    Optional<AircraftModel> findByModelNameAndManufacturer(String modelName, String manufacturer);
    Optional<AircraftModel> findById(String id);
    Optional<AircraftModel> findById(AircraftModelId id);

}

