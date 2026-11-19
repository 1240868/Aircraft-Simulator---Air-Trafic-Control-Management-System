package eapli.aisafe.fleet.repositories;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.framework.domain.repositories.DomainRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AircraftRepository extends DomainRepository<RegistrationNumber, Aircraft> {

    Optional<Aircraft> findByRegistration(RegistrationNumber registration);
    Optional<Aircraft> findByRegistration(String registration);

    Iterable<Aircraft> findByOwnerCompanyId(CompanyId companyId);

    /**
     * Active (non-retired) aircraft of the given model owned by the company (US072a).
     */
    Iterable<Aircraft> findActiveByOwnerCompanyAndModel(CompanyId companyId, AircraftModelId modelId);

    /**
     * Active (non-retired) aircraft owned by the company whose total configured seats equal {@code passengerCapacity}.
     */
    Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(CompanyId companyId, int passengerCapacity);

    /**
     * Active (non-retired) aircraft owned by the company whose complete age in years equals {@code completeYears}
     * as of {@code asOf} (see {@link eapli.aisafe.fleet.domain.Aircraft#isListedForActiveFleetByAge}).
     */
    Iterable<Aircraft> findActiveByOwnerCompanyAndAge(CompanyId companyId, int completeYears, LocalDate asOf);
}
