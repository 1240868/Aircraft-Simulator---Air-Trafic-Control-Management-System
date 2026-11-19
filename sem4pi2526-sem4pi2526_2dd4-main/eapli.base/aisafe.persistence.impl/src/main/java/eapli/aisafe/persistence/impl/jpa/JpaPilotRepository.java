package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class JpaPilotRepository
        extends BaseJpaRepositoryBase<Pilot, PilotId, PilotId>
        implements PilotRepository {

    JpaPilotRepository() {
        super("id");
    }

    @Override
    public Optional<Pilot> findByEmail(final Email email) {
        return matchOne("e.email.value = :emailVal", "emailVal", email.value());
    }

    @Override
    public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("status", PilotStatus.ACTIVE);
        return match("e.company = :companyId AND e.status = :status", params);
    }

    @Override
    public Optional<Pilot> ofIdentity(final String id) {
        return matchOne("e.id.value = :id", "id", id);
    }
}
