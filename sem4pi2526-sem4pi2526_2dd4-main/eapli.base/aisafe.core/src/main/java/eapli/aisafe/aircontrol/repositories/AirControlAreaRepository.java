package eapli.aisafe.aircontrol.repositories;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface AirControlAreaRepository extends DomainRepository<AirControlAreaCode, AirControlArea> {

    Optional<AirControlArea> findByCode(AirControlAreaCode code);

    Optional<AirControlArea> findByCollaboratorUsername(String username);
}
