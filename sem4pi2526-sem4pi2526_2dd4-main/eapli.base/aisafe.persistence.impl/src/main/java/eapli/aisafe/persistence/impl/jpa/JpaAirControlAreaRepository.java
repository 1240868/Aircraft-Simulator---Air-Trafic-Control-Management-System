package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;

import java.util.Optional;

class JpaAirControlAreaRepository
        extends BaseJpaRepositoryBase<AirControlArea, AirControlAreaCode, AirControlAreaCode>
        implements AirControlAreaRepository {

    JpaAirControlAreaRepository() {
        super("code");
    }

    @Override
    public Optional<AirControlArea> findByCode(final AirControlAreaCode code) {
        return matchOne("e.code=:code", "code", code);
    }

    @Override
    public Optional<AirControlArea> findByCollaboratorUsername(final String username) {
        return matchOne(
                "EXISTS (SELECT c FROM e.collaborators c WHERE c.username = :u)",
                "u", username);
    }
}
