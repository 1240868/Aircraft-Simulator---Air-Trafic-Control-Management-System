package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import java.util.List;
import java.util.Optional;

class JpaAirTransportCompanyRepository
        extends BaseJpaRepositoryBase<AirTransportCompany, CompanyId, CompanyId>
        implements AirTransportCompanyRepository {

    JpaAirTransportCompanyRepository() {
        super("id");
    }

    @Override
    public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
        return matchOne("UPPER(e.iataCode.value) = :code", "code", iataCode.trim().toUpperCase());
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
        return matchOne("UPPER(e.icaoCode.value) = :code", "code", icaoCode.trim().toUpperCase());
    }

    @Override
    public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
        final String username = systemUsername == null ? "" : systemUsername.strip();
        if (username.isEmpty()) {
            return Optional.empty();
        }
        final List<AirTransportCompany> matches = entityManager()
                .createQuery("SELECT e FROM AirTransportCompany e JOIN e.collaborators c "
                        + "WHERE c.username = :username", AirTransportCompany.class)
                .setParameter("username", username)
                .getResultList();
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0).identity());
    }

    @Override
    public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
        return matchOne("e.id.value = :id", "id", companyId)
                .map(AirTransportCompany::iataCode);
    }
}
