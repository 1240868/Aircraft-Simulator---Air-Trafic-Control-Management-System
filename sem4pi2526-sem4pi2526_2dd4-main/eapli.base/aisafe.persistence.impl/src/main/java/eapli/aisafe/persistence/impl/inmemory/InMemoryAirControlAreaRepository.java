package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryAirControlAreaRepository
        extends InMemoryDomainRepository<AirControlArea, AirControlAreaCode>
        implements AirControlAreaRepository {

    @Override
    public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
        return matchOne(e -> e.identity().equals(code));
    }

    @Override
    public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
        return matchOne(e -> e.collaboratorUsernames().contains(username));
    }
}
