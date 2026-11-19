package eapli.aisafe.fleet.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

/** In-memory doubles for fleet listing / decommission controller tests. */
final class FleetListingTestDoubles {

    private FleetListingTestDoubles() {
    }

    static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;
        String username = "atcc";

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    static final class TestCompanyRepo extends InMemoryDomainRepository<AirTransportCompany, CompanyId>
            implements AirTransportCompanyRepository {

        Optional<CompanyId> linkedCompany = Optional.empty();

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            return linkedCompany;
        }

        @Override
        public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
            return Optional.empty();
        }

        @Override
        public Optional<CompanyIataCode> findIataByCompanyId(final String companyId) {
            for (final var c : findAll()) {
                if (c.identity().toString().equals(companyId)) {
                    return Optional.of(c.iataCode());
                }
            }
            return Optional.empty();
        }
    }

    static final class TestModelRepo extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                       final String manufacturer) {
            return Optional.empty();
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            final AircraftModelId mid = new AircraftModelId(UUID.fromString(id));
            return matchOne(e -> e.identity().equals(mid));
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            return matchOne(e -> e.identity().equals(id));
        }
    }

    static final class TestAircraftRepo extends InMemoryDomainRepository<Aircraft, RegistrationNumber>
            implements AircraftRepository {

        final List<Aircraft> activeByModel = new ArrayList<>();
        final List<Aircraft> activeByCapacity = new ArrayList<>();
        final List<Aircraft> activeByAge = new ArrayList<>();
        final List<Aircraft> byCompany = new ArrayList<>();
        final List<Aircraft> saved = new ArrayList<>();

        @Override
        public Optional<Aircraft> findByRegistration(final RegistrationNumber registration) {
            return ofIdentity(registration);
        }

        @Override
        public Optional<Aircraft> findByRegistration(final String registration) {
            for (final Aircraft a : findAll()) {
                if (a.identity().toString().equals(registration)) {
                    return Optional.of(a);
                }
            }
            return Optional.empty();
        }

        @Override
        public Iterable<Aircraft> findByOwnerCompanyId(final CompanyId companyId) {
            return List.copyOf(byCompany);
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndModel(final CompanyId companyId,
                                                                   final AircraftModelId modelId) {
            return List.copyOf(activeByModel);
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndPassengerCapacity(final CompanyId companyId,
                                                                               final int passengerCapacity) {
            return List.copyOf(activeByCapacity);
        }

        @Override
        public Iterable<Aircraft> findActiveByOwnerCompanyAndAge(final CompanyId companyId,
                                                                 final int completeYears,
                                                                 final LocalDate asOf) {
            return List.copyOf(activeByAge);
        }

        @Override
        public Aircraft save(final Aircraft entity) {
            saved.add(entity);
            return super.save(entity);
        }
    }

    static final class FakePendingFlights implements PendingFlightsChecker {
        final List<RegistrationNumber> pending = new ArrayList<>();

        @Override
        public boolean hasPendingFlights(final RegistrationNumber registration) {
            return pending.stream().anyMatch(p -> p.equals(registration));
        }
    }
}
