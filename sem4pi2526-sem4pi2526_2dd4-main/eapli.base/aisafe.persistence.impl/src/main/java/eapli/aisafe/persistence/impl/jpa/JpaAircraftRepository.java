package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftOperationalStatus;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class JpaAircraftRepository
        extends BaseJpaRepositoryBase<Aircraft, RegistrationNumber, RegistrationNumber>
        implements AircraftRepository {

    JpaAircraftRepository() {
        super("registrationNumber");
    }

    @Override
    public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
        return ofIdentity(registration);
    }

    @Override
    public Optional<Aircraft> findByRegistration(final String registration) {
        return matchOne("e.registrationNumber.value = :reg", "reg", registration);
    }

    @Override
    public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
        return match("e.ownerCompanyId = :companyId", "companyId", companyId);
    }

    @Override
    public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                               final AircraftModelId modelId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("modelId", modelId);
        params.put("retired", AircraftOperationalStatus.RETIRED);
        return match(
                "e.ownerCompanyId = :companyId AND e.aircraftModelId = :modelId "
                        + "AND e.operationalStatus <> :retired",
                params);
    }

    @Override
    public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                           final int passengerCapacity) {
        final List<Aircraft> out = new ArrayList<>();
        for (final Aircraft a : findNonRetiredByOwner(companyId)) {
            if (a.isListedForActiveFleetByPassengerCapacity(companyId, passengerCapacity)) {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public Iterable<Aircraft> findActiveByOwnerCompanyAndAge(final CompanyId companyId,
                                                             final int completeYears,
                                                             final LocalDate asOf) {
        final List<Aircraft> out = new ArrayList<>();
        for (final Aircraft a : findNonRetiredByOwner(companyId)) {
            if (a.isListedForActiveFleetByAge(companyId, completeYears, asOf)) {
                out.add(a);
            }
        }
        return out;
    }

    private List<Aircraft> findNonRetiredByOwner(final CompanyId companyId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("retired", AircraftOperationalStatus.RETIRED);
        return match(
                "e.ownerCompanyId = :companyId AND e.operationalStatus <> :retired",
                params);
    }
}
