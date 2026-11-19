package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, RegistrationNumber>
        implements AircraftRepository {

    @Override
    public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
        return ofIdentity(registration);
    }

    @Override
    public Optional<Aircraft> findByRegistration(final String registration) {
        for (final var a : super.findAll()) {
            if (a.identity().toString().equals(registration)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
        final List<Aircraft> out = new ArrayList<>();
        for (final var a : super.findAll()) {
            if (a.ownerCompanyId().equals(companyId)) {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                                final AircraftModelId modelId) {
        final List<Aircraft> out = new ArrayList<>();
        for (final var a : super.findAll()) {
            if (a.isListedForActiveFleetByModel(companyId, modelId)) {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                           final int passengerCapacity) {
        final List<Aircraft> out = new ArrayList<>();
        for (final var a : super.findAll()) {
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
        for (final var a : super.findAll()) {
            if (a.isListedForActiveFleetByAge(companyId, completeYears, asOf)) {
                out.add(a);
            }
        }
        return out;
    }
}
