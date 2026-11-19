# US076 — Design

## 1. Overview

This document describes the detailed software design for **US076 — List company pilot roster**, following a **Layered Architecture** with **DDD tactical patterns**.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_76/design/`.

Diagrams: [cd_us_76.puml](cd_us_76.puml), [sd_us_76.puml](sd_us_76.puml).

**Current status:** implemented and covered by unit tests (domain, application, persistence, console UI).

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│           ListPilotRosterUI (Console)                │
└───────────────────────┬─────────────────────────────┘
                        │ uses DTO
┌───────────────────────▼─────────────────────────────┐
│                 APPLICATION LAYER                    │
│  ListPilotRosterController                         │
│  PilotRosterListing (static helper)                │
│  PilotRosterListItemDTO / PilotRosterListResult    │
└──────────┬────────────────────────┬─────────────────┘
           │ uses domain objects    │ calls
┌──────────▼────────────┐  ┌───────▼─────────────────┐
│     DOMAIN LAYER      │  │   REPOSITORY INTERFACES  │
│  Pilot (AR)           │  │  PilotRepository         │
│  PilotStatus          │  │  AirTransportCompany     │
│  PilotId, License,... │  │    Repository            │
└───────────────────────┘  │  AircraftModelRepository │
                           └───────┬─────────────────┘
                                   │ implemented by
                           ┌───────▼─────────────────┐
                           │  INFRASTRUCTURE LAYER    │
                           │  JpaPilotRepository      │
                           │  InMemoryPilotRepository │
                           │  AuthorizationPort       │
                           │  (FrameworkAuthorization │
                           │   Adapter)               │
                           └─────────────────────────┘
```

### Package mapping

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **core.domain** | `aisafe.core` | `eapli.aisafe.pilot`, `eapli.aisafe.pilot.repositories` |
| **application** | `aisafe.infrastructure.application` | `eapli.aisafe.pilot.application`, `.dto` |
| **infrastructure.persistence** | `aisafe.persistence.impl` | `eapli.aisafe.persistence.impl.jpa` / `.inmemory` |

---

## 3. Class Design

### 3.1 Domain Layer — `aisafe.core`

#### `Pilot` — Aggregate Root

```java
@Entity
public class Pilot implements AggregateRoot<PilotId> {

    private PilotId id;
    private FullName name;
    private Email email;
    private CompanyId company;          // reference by identity
    private PilotCertification certification;
    private License licenseVoo;
    private PilotStatus status;
    private List<AircraftModelId> certifiedModels;

    /** US076 — only ACTIVE pilots appear on the company roster. */
    public boolean isEligibleForRosterListing() {
        return status == PilotStatus.ACTIVE;
    }

    public CompanyId getCompany() { return company; }
    public List<AircraftModelId> getCertifiedModels() {
        return Collections.unmodifiableList(certifiedModels);
    }
    // ...
}
```

**US076 invariants (tested in `PilotRosterListingTest`):**

| ID | Invariant |
|----|-----------|
| INV-1 | Only `PilotStatus.ACTIVE` is eligible (`isEligibleForRosterListing() == true`) |
| INV-2 | `getCertifiedModels()` returns an immutable list |

**References by identity:**

- `Pilot` → `CompanyId` (roster scope; does not modify `AirTransportCompany`)
- `Pilot` → `AircraftModelId` (certifications; labels resolved via `AircraftModelRepository` in DTO)
- `UserId` — **conceptual** reference from the global DDD model; in the US075 implementation identity is reflected via embedded `Email` + `FullName`

#### `PilotStatus` — Enum

```java
public enum PilotStatus {
    ACTIVE, INACTIVE, SUSPENDED, RETIRED
}
```

---

### 3.2 Repository Interface — Domain Layer

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {

    Optional<Pilot> findByEmail(Email email);

    /** US076 — active pilots registered for the given air transport company. */
    Iterable<Pilot> findActiveByCompanyId(CompanyId companyId);
}
```

```java
public interface AirTransportCompanyRepository
        extends DomainRepository<CompanyId, AirTransportCompany> {

    Optional<CompanyId> findCompanyIdForCollaboratorUsername(String username);
}
```

The `findActiveByCompanyId` contract is responsible for filtering `ACTIVE` pilots for the given company. Domain eligibility (`isEligibleForRosterListing`) complements the rule in aggregate tests.

---

### 3.3 Application Layer — `aisafe.infrastructure.application`

#### `ListPilotRosterController`

```java
@UseCaseController
public class ListPilotRosterController {

    private final AuthorizationPort authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final AircraftModelRepository modelRepo;

    public ListPilotRosterController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
             PersistenceContext.repositories().pilots(),
             PersistenceContext.repositories().airTransportCompanies(),
             PersistenceContext.repositories().aircraftModels());
    }

    public PilotRosterListResult listPilotRoster() {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final String username = authz.currentUsername();
        final CompanyId companyId =
            PilotRosterListing.requireCompanyOfUser(companyRepo, username);

        final List<PilotRosterListItemDTO> rows = new ArrayList<>();
        for (final Pilot pilot : pilotRepo.findActiveByCompanyId(companyId)) {
            rows.add(PilotRosterListing.toRow(pilot, modelRepo));
        }
        rows.sort(Comparator.comparing(PilotRosterListItemDTO::fullName));

        final String messageWhenEmpty = rows.isEmpty()
            ? PilotRosterListResult.NO_ACTIVE_PILOTS
            : null;

        return new PilotRosterListResult(List.copyOf(rows), messageWhenEmpty);
    }
}
```

#### `PilotRosterListing` — Application Helper

```java
public final class PilotRosterListing {

    public static CompanyId requireCompanyOfUser(
            AirTransportCompanyRepository companyRepo,
            String username) {
        return companyRepo.findCompanyIdForCollaboratorUsername(username)
            .orElseThrow(() -> new IllegalStateException(
                "Authenticated user is not linked to any air transport company"));
    }

    public static PilotRosterListItemDTO toRow(Pilot pilot,
                                               AircraftModelRepository modelRepo) {
        return new PilotRosterListItemDTO(
            pilot.identity().toString(),
            pilot.getName().firstName() + " " + pilot.getName().lastName(),
            pilot.getEmail().value(),
            pilot.getCertification().value(),
            pilot.getLicenseVoo().value(),
            pilot.getLicenseVoo().expirationDate().toString(),
            pilot.getStatus().name(),
            resolveCertifiedModelLabels(pilot, modelRepo)
        );
    }

    static List<String> resolveCertifiedModelLabels(Pilot pilot,
                                                    AircraftModelRepository modelRepo) {
        // maps each AircraftModelId to "modelName / manufacturer", or id if not found
    }
}
```

#### Output DTOs

```java
public record PilotRosterListItemDTO(
    String pilotId,
    String fullName,
    String email,
    String certification,
    String licenseNumber,
    String licenseExpiration,
    String status,
    List<String> certifiedModels
) {}
```

```java
public record PilotRosterListResult(
    List<PilotRosterListItemDTO> pilots,
    String messageWhenEmpty
) {
    public static final String NO_ACTIVE_PILOTS =
        "There are no active pilots in your company's roster.";
}
```

DTOs **decouple** presentation from the `Pilot` aggregate: the UI consumes only serializable data, without depending on JPA entities or domain methods.

---

### 3.4 Infrastructure Layer — `aisafe.persistence.impl`

#### Abstract Factory (RepositoryFactory pattern)

```java
public interface RepositoryFactory {
    PilotRepository pilots();
    AirTransportCompanyRepository airTransportCompanies();
    AircraftModelRepository aircraftModels();
    // ...
}
```

Implementations: `JpaRepositoryFactory`, `InMemoryRepositoryFactory`.

#### `InMemoryPilotRepository` / `JpaPilotRepository`

```java
@Override
public Iterable<Pilot> findActiveByCompanyId(CompanyId companyId) {
    // filter: pilot.getCompany().equals(companyId)
    //     && pilot.isEligibleForRosterListing()
}
```

#### `AuthorizationPort`

```java
public interface AuthorizationPort {
    void ensureAuthenticatedUserHasAnyOf(Role... roles);
    String currentUsername();
}
```

Production implementation: `FrameworkAuthorizationAdapter.usingRegistry()`.

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
ATCC → UI: menu "List company pilot roster"
UI → Controller: listPilotRoster()
Controller → AuthorizationPort: ensureAuthenticatedUserHasAnyOf(ATCC)
Controller → AuthorizationPort: currentUsername()
Controller → PilotRosterListing: requireCompanyOfUser(companyRepo, username)
  PilotRosterListing → AirTransportCompanyRepository: findCompanyIdForCollaboratorUsername
  Repository ← companyId
Controller → PilotRepository: findActiveByCompanyId(companyId)
  Repository ← Iterable<Pilot> (ACTIVE only)
loop each pilot
  Controller → PilotRosterListing: toRow(pilot, modelRepo)
  PilotRosterListing → AircraftModelRepository: findById per certified model
  PilotRosterListing ← PilotRosterListItemDTO
Controller → Controller: sort by fullName
Controller → PilotRosterListResult: build result
Controller ← PilotRosterListResult
UI ← Controller: display roster (or NO_ACTIVE_PILOTS)
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository** | `PilotRepository`, `AirTransportCompanyRepository` | Abstracts persistence; collection-like interface |
| **Abstract Factory** | `RepositoryFactory` | Swaps in-memory ↔ JPA via configuration |
| **DTO** | `PilotRosterListItemDTO`, `PilotRosterListResult` | Decouples domain from presentation |
| **Value Object** | `PilotId`, `CompanyId`, `FullName`, `Email`, `License` | Immutability and encapsulated validation |
| **Aggregate Root** | `Pilot` | Encapsulates roster eligibility (`isEligibleForRosterListing`) |
| **Use Case Controller** | `@UseCaseController` on `ListPilotRosterController` | EAPLI pattern for use cases |
| **Test Double (manual)** | `FakeAuth`, `TestPilotRepo`, `TestCompanyRepo` | Full DB isolation in tests (NFR08) |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `AuthzException` | User without `AIR_TRANSPORT_COMPANY_COLLABORATOR` role | Application |
| `IllegalStateException` | ATCC not linked to any company (`"not linked"`) | Application |

---

## 7. TDD Contract

The design was **guided by unit tests written before implementation**. Traceability detail: [tests/testes.md](../tests/testes.md).

| Scenario | Test class |
|----------|------------|
| `ACTIVE` eligibility / exclusion of other states | `PilotRosterListingTest` |
| Success with sorting and DTO mapping | `ListPilotRosterControllerTest` |
| Empty list with `NO_ACTIVE_PILOTS` | `ListPilotRosterControllerTest` |
| ATCC `companyId` scope | `ListPilotRosterControllerTest` |
| Inactive exclusion via repository | `ListPilotRosterControllerTest` |
| Failure when ATCC has no company | `ListPilotRosterControllerTest` |
| Read-only operation | `ListPilotRosterControllerTest` |
