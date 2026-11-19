package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryPilotRepository
        extends InMemoryDomainRepository<Pilot, PilotId>
        implements PilotRepository {

    @Override
    public Optional<Pilot> findByEmail(final Email email) {
        for (final Pilot p : findAll()) {
            if (p.getEmail().value().equals(email.value())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Pilot> findActiveByCompanyId(final CompanyId companyId) {
        final List<Pilot> active = new ArrayList<>();
        for (final Pilot pilot : findAll()) {
            if (pilot.getCompany().equals(companyId) && pilot.isEligibleForRosterListing()) {
                active.add(pilot);
            }
        }
        return active;
    }

    @Override
    public Optional<Pilot> ofIdentity(final String id) {
        for (final var p : super.findAll()) {
            if (p.identity().toString().equals(id)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
