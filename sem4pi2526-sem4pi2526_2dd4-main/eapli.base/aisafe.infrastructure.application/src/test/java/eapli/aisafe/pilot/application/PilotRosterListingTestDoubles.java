package eapli.aisafe.pilot.application;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotId;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory doubles for pilot application controller tests.
 * Replaces Mockito — follows the project's manual test-double pattern.
 */
final class PilotRosterListingTestDoubles {

    private PilotRosterListingTestDoubles() {
    }

    static final class FakeAuth implements AuthorizationPort {
        Role[] lastRoles;
        String username = "atcc";
        boolean denyAuthorization;

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            if (denyAuthorization) {
                throw new RuntimeException("Authenticated user is not authorized");
            }
            this.lastRoles = roles;
        }

        @Override
        public String currentUsername() {
            return username;
        }
    }

    static final class TestCompanyRepo
            extends InMemoryDomainRepository<eapli.aisafe.company.domain.AirTransportCompany, CompanyId>
            implements AirTransportCompanyRepository {

        Optional<CompanyId> linkedCompany = Optional.empty();

        @Override
        public Optional<CompanyId> findCompanyIdForCollaboratorUsername(final String systemUsername) {
            return linkedCompany;
        }

        @Override
        public Optional<eapli.aisafe.company.domain.AirTransportCompany> findByIataCode(final String iataCode) {
            return Optional.empty();
        }

        @Override
        public Optional<eapli.aisafe.company.domain.AirTransportCompany> findByIcaoCode(final String icaoCode) {
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

    static final class TestPilotRepo
            extends InMemoryDomainRepository<Pilot, PilotId>
            implements PilotRepository {

        CompanyId lastQueriedCompanyId;
        Pilot lastSavedPilot;
        int saveCount;

        @Override
        public Pilot save(final Pilot entity) {
            final boolean isUpdate = ofIdentity(entity.identity()).isPresent();
            lastSavedPilot = entity;
            final Pilot saved = super.save(entity);
            if (isUpdate) {
                saveCount++;
            }
            return saved;
        }

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
            lastQueriedCompanyId = companyId;
            final List<Pilot> active = new ArrayList<>();
            for (final Pilot pilot : findAll()) {
                if (companyId.equals(pilot.getCompany()) && pilot.isEligibleForRosterListing()) {
                    active.add(pilot);
                }
            }
            return List.copyOf(active);
        }

        @Override
        public Optional<Pilot> ofIdentity(final String id) {
            final PilotId pid = new PilotId(UUID.fromString(id));
            return matchOne(p -> p.identity().equals(pid));
        }
    }

    static final class TestAircraftModelRepo implements AircraftModelRepository {
        final Map<AircraftModelId, AircraftModel> store = new HashMap<>();

        @Override
        public AircraftModel save(final AircraftModel entity) {
            store.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AircraftModel> ofIdentity(final AircraftModelId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Iterable<AircraftModel> findAll() {
            return store.values();
        }

        @Override
        public void delete(final AircraftModel entity) {
            store.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final AircraftModelId id) {
            store.remove(id);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public Optional<AircraftModel> findByModelNameAndManufacturer(final String modelName,
                                                                    final String manufacturer) {
            return store.values().stream()
                    .filter(m -> m.modelName().equals(modelName) && m.manufacturer().equals(manufacturer))
                    .findFirst();
        }

        @Override
        public Optional<AircraftModel> findById(final String id) {
            return store.values().stream()
                    .filter(m -> m.identity().toString().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<AircraftModel> findById(final AircraftModelId id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    static final class TestFlightPlanRepo
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {

        final Set<PilotId> pilotsWithAssignedPlans = new HashSet<>();
        PilotId lastCheckedPilotId;

    @Override
    public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
        lastCheckedPilotId = pilotId;
        return pilotsWithAssignedPlans.contains(pilotId);
    }

    @Override
    public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
        return List.of();
    }
}
}
