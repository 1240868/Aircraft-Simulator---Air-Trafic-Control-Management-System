package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryAirTransportCompanyRepository
        extends InMemoryDomainRepository<AirTransportCompany, CompanyId>
        implements AirTransportCompanyRepository {

    @Override
    public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
        final String key = iataCode.trim().toUpperCase();
        for (final var c : super.findAll()) {
            if (c.iataCode().value().equals(key)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
        final String key = icaoCode.trim().toUpperCase();
        for (final var c : super.findAll()) {
            if (c.icaoCode().value().equals(key)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
        final String u = systemUsername == null ? "" : systemUsername.strip();
        if (u.isEmpty()) {
            return Optional.empty();
        }
        for (final var c : super.findAll()) {
            if (c.collaboratorUsernames().contains(u)) {
                return Optional.of(c.identity());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
        for (final var c : super.findAll()) {
            if (c.identity().toString().equals(companyId)) {
                return Optional.of(c.iataCode());
            }
        }
        return Optional.empty();
    }
}
