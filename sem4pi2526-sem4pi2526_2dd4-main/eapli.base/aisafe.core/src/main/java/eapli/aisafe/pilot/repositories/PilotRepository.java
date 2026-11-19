package eapli.aisafe.pilot.repositories;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> ofIdentity (String id);

    Optional<Pilot> findByEmail(Email email);

    Iterable<Pilot> findActiveByCompanyId(CompanyId companyId);
}
