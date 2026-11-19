package eapli.aisafe.company.repositories;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface AirTransportCompanyRepository extends DomainRepository<CompanyId, AirTransportCompany> {
    Optional<AirTransportCompany> findByIataCode(String iataCode);

    Optional<AirTransportCompany> findByIcaoCode(String icaoCode);

    /**
     * Resolves which company lists this system user as an active collaborator (username string).
     */
    Optional<CompanyId> findCompanyIdForCollaboratorUsername(String systemUsername);
    Optional<CompanyIataCode> findIataByCompanyId (String companyId);
}
