package eapli.aisafe.company.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.time.LocalDate;

@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationPort authz;
    private final AirTransportCompanyRepository repo;

    public RegisterAirTransportCompanyController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    public RegisterAirTransportCompanyController(final AuthorizationPort authz,
                                                 final AirTransportCompanyRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    public AirTransportCompany register(final String companyName,
                                        final String iataCode,
                                        final String icaoCode,
                                        final String certificateNumber,
                                        final LocalDate certificateExpiry) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN, Roles.BACKOFFICE_OPERATOR);

        if (repo.findByIataCode(iataCode).isPresent()) {
            throw new IntegrityViolationException("IATA code already registered: " + iataCode);
        }
        if (repo.findByIcaoCode(icaoCode).isPresent()) {
            throw new IntegrityViolationException("ICAO code already registered: " + icaoCode);
        }

        final var company = new AirTransportCompany(
                new CompanyName(companyName),
                new CompanyIataCode(iataCode),
                new CompanyIcaoCode(icaoCode),
                new AirOperatorCertificate(certificateNumber, certificateExpiry)
        );
        return repo.save(company);
    }
}
