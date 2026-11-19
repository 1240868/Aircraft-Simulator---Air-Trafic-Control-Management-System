# US076 — List company pilot roster

## 1. Context

As an **Air Transport Company Collaborator (ATCC)**, I want to list my company's pilot roster to see who is active and available for operations.

The operation is **read-only**: no aggregate is modified. The listing is restricted to pilots with `PilotStatus.ACTIVE` belonging to the authenticated collaborator's `CompanyId`.

**Dependencies:** US030, US060/061, US075.

**Current status:** Implemented (domain, application, persistence, console UI and tests).

Supporting documentation: [requirements/requisitos.md](requirements/requisitos.md), [design/design.md](design/design.md), [analyse/analise.md](analyse/analise.md), [tests/testes.md](tests/testes.md).

---

## 2. Requirements

**US076** As an Air Transport Company Collaborator, I want to list my company's pilot roster.

**Acceptance criteria (derived from tests):**

| ID | Criterion | Traceability test |
|----|-----------|-------------------|
| AC-01 | Only `ACTIVE` pilots appear in the roster | `PilotRosterListingTest` [INV-1] |
| AC-02 | `INACTIVE`, `SUSPENDED`, `RETIRED` pilots excluded | `inactive/suspended/retired...` |
| AC-03 | Listing restricted to the ATCC's company (`companyId`) | `listShouldQueryPilotsByAuthenticatedCompany` |
| AC-04 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` required | `listShouldReturnActivePilotsSortedByFullName` |
| AC-05 | ATCC not linked to a company → failure | `listShouldFailWhenUserNotLinkedToCompany` |
| AC-06 | Empty list with `NO_ACTIVE_PILOTS` message | `listShouldReturnEmptyResultWithMessage` |
| AC-07 | Results sorted by full name | `listShouldReturnActivePilotsSortedByFullName` |
| AC-08 | Operation does not mutate persisted data | `listShouldNotMutatePersistedPilots` |
| AC-09 | Certified aircraft models listed (name / manufacturer) | `listShouldReturnActivePilotsSortedByFullName` |

**Role:** `AIR_TRANSPORT_COMPANY_COLLABORATOR`.

---

## 3. Analysis

### 3.1 Actor

| Actor | System role |
|-------|-------------|
| **Air Transport Company Collaborator** | Authenticated user with access to the company's operational management (fleet, pilots, routes) |

### 3.2 Business rules (domain)

Validated in `PilotRosterListingTest`:

- **BR-01:** A pilot is eligible for the roster only if `Pilot.isEligibleForRosterListing()` returns `true`, which requires `PilotStatus.ACTIVE`.
- **BR-02:** `INACTIVE`, `SUSPENDED`, and `RETIRED` states are always excluded.
- **BR-03:** The certified models list is immutable within the aggregate (`getCertifiedModels()` unmodifiable).

### 3.3 Application rules

Validated in `ListPilotRosterControllerTest`:

- **BR-04:** The query `PilotRepository.findActiveByCompanyId(companyId)` uses exclusively the `CompanyId` resolved from the authenticated ATCC's username.
- **BR-05:** Presentation mapping is done via DTOs, without exposing the `Pilot` aggregate to the presentation layer.

### 3.4 Validations

| # | Component | Check | Where | On failure |
|---|-----------|-------|-------|------------|
| 1 | `ListPilotRosterController` | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `listPilotRoster()` — `AuthorizationPort` | `AuthzException` |
| 2 | `PilotRosterListing` | ATCC linked to a company | `requireCompanyOfUser()` | `IllegalStateException` |
| 3 | `PilotRepository` | Active pilots for `companyId` | `findActiveByCompanyId()` | — |
| 4 | `PilotRosterListing` | Mapping to DTO | `toRow(pilot, modelRepo)` | — |
| 5 | `ListPilotRosterController` | Sort by `fullName` | post-mapping | — |
| 6 | `PilotRosterListResult` | Message when empty | result construction | — |

---

## 4. Design

### 4.1 Layered architecture

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `ListPilotRosterUI` | Invokes the controller and displays the roster |
| **Application** | `ListPilotRosterController` | Orchestrates authorization, query, and mapping |
| **Application** | `PilotRosterListing` | Resolves `CompanyId` and maps `Pilot` → DTO |
| **Application** | `PilotRosterListItemDTO`, `PilotRosterListResult` | Data transport for the UI |
| **Domain** | `Pilot` (AR), VOs, `PilotStatus` | Eligibility and encapsulation |
| **Infrastructure** | `PilotRepository` (+ JPA/InMemory impl) | `findActiveByCompanyId` |
| **Infrastructure** | `AircraftModelRepository` | Resolve certified model labels for DTO |
| **Infrastructure** | `AuthorizationPort` | Authentication and roles (US030) |

### 4.2 Class diagram

Source: [design/cd_us_76.puml](design/cd_us_76.puml)  
Generated: [design/CD_US076_ListPilotRoster.svg](design/CD_US076_ListPilotRoster.svg)

Includes `ListPilotRosterController`, `AircraftModelRepository`, `PilotRosterListing.toRow(pilot, modelRepo)`, `ListPilotRosterUI`, and DTO `certifiedModels`.

### 4.3 Sequence diagram

Source: [design/sd_us_76.puml](design/sd_us_76.puml)  
Generated: [design/SD_US076_ListPilotRoster.svg](design/SD_US076_ListPilotRoster.svg)

Regenerate SVGs from the repo root (requires Java 11+): `./generate-plantuml-diagrams.sh`

### 4.4 Flow

1. `ListPilotRosterUI` invokes `ListPilotRosterController.listPilotRoster()`.
2. Controller verifies role `AIR_TRANSPORT_COMPANY_COLLABORATOR`.
3. Obtains `username` and resolves `CompanyId` via `PilotRosterListing.requireCompanyOfUser` → `AirTransportCompanyRepository`.
4. Invokes `PilotRepository.findActiveByCompanyId(companyId)`.
5. For each `Pilot`, `PilotRosterListing.toRow(pilot, modelRepo)` produces a `PilotRosterListItemDTO` with certified model labels.
6. Sorts by `fullName` and returns `PilotRosterListResult` (with `NO_ACTIVE_PILOTS` if empty).

### 4.5 DTOs and low coupling

| DTO | Main fields | Purpose |
|-----|-------------|---------|
| `PilotRosterListItemDTO` | `pilotId`, `fullName`, `email`, `certification`, `licenseNumber`, `licenseExpiration`, `status`, `certifiedModels` | One roster row |
| `PilotRosterListResult` | `pilots`, `messageWhenEmpty` | Aggregated listing result |

DTOs **decouple** presentation from the `Pilot` aggregate: the UI does not access JPA entities or domain methods directly, consuming only serializable data. Mapping is the application layer's responsibility (`PilotRosterListing`), tested in `ListPilotRosterControllerTest`.

---

## 5. Test strategy (TDD)

The design was **guided by unit tests written before business implementation**, in line with the TDD approach adopted in the project.

### 5.1 Covered scenarios

| Scenario | Test | Layer |
|----------|------|-------|
| **Success with sorting** | `listShouldReturnActivePilotsSortedByFullName` | Application |
| **Empty list** | `listShouldReturnEmptyResultWithMessage` | Application |
| **Inactive exclusion** | `listShouldRelyOnRepositoryToExcludeInactivePilots` + [INV-1] tests | Application + Domain |
| **Context/authorization failure** | `listShouldFailWhenUserNotLinkedToCompany` | Application |
| **ACTIVE eligibility** | `activePilotShouldBeEligibleForRosterListing` | Domain |
| **Read-only** | `listShouldNotMutatePersistedPilots` | Application |

Full detail: [tests/testes.md](tests/testes.md).

### 5.2 Test doubles (NFR03 / NFR08 isolation)

| Double | Interface | File |
|--------|-----------|------|
| `FakeAuth` | `AuthorizationPort` | `PilotRosterListingTestDoubles` |
| `TestPilotRepo` | `PilotRepository` | `PilotRosterListingTestDoubles` |
| `TestCompanyRepo` | `AirTransportCompanyRepository` | `PilotRosterListingTestDoubles` |
| `TestAircraftModelRepo` | `AircraftModelRepository` | `PilotRosterListingTestDoubles` |

**Manual** test doubles (no Mockito), injected via the controller constructor — US072/US075 pattern. They ensure full database isolation in unit tests.

**Note:** there are no dedicated DTO/Record tests; mapping is validated in controller tests.

---

## 6. Implementation

| Layer | Files | Status |
|-------|-------|--------|
| **Domain** | `Pilot.java` (+ `isEligibleForRosterListing`) | ✅ Done |
| **Domain** | `PilotRepository.java` (+ `findActiveByCompanyId`) | ✅ Done |
| **Application** | `ListPilotRosterController.java` | ✅ Done |
| **Application** | `PilotRosterListing.java` | ✅ Done |
| **Application / DTO** | `dto/PilotRosterListItemDTO.java`, `dto/PilotRosterListResult.java` | ✅ Done |
| **Persistence** | `InMemoryPilotRepository`, `JpaPilotRepository` | ✅ Done |
| **Presentation** | `ListPilotRosterUI` | ✅ Done |

### 6.1 Package locations

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **core.domain** | `aisafe.core` | `eapli.aisafe.pilot`, `eapli.aisafe.pilot.repositories` |
| **application** | `aisafe.infrastructure.application` | `eapli.aisafe.pilot.application`, `.dto` |
| **infrastructure.persistence** | `aisafe.persistence.impl` | `eapli.aisafe.persistence.impl.jpa` / `.inmemory` |

This organization follows the AISafe **layered architecture**: domain independent of frameworks, application with use cases, infrastructure with persistence implementations.

---

## 7. Integration / demonstration

1. Bootstrap: company + ATCC + active and inactive pilots (US060/061/075).
2. Login as ATCC.
3. Menu: "List company pilot roster".
4. Verify: only `ACTIVE` pilots from the company, sorted by name.
5. Deactivate a pilot (US077) and confirm exclusion from the roster.

```bash
plantuml docs/us_76/design/cd_us_76.puml docs/us_76/design/sd_us_76.puml
mvn test -pl aisafe.core,aisafe.infrastructure.application \
  -Dtest=PilotRosterListingTest,ListPilotRosterControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

---

## 8. Files created

### Documentation
- `docs/us_76/readme.md`
- `docs/us_76/requirements/requisitos.md`
- `docs/us_76/design/design.md`
- `docs/us_76/analyse/analise.md`
- `docs/us_76/tests/testes.md`
- `docs/us_76/design/cd_us_76.puml`
- `docs/us_76/design/sd_us_76.puml`

### Code (US076)
- `aisafe.core/.../pilot/Pilot.java` (`isEligibleForRosterListing` method)
- `aisafe.core/.../pilot/repositories/PilotRepository.java`
- `aisafe.core/.../test/.../PilotRosterListingTest.java`
- `aisafe.infrastructure.application/.../ListPilotRosterController.java`
- `aisafe.infrastructure.application/.../PilotRosterListing.java`
- `aisafe.infrastructure.application/.../dto/PilotRosterListItemDTO.java`
- `aisafe.infrastructure.application/.../dto/PilotRosterListResult.java`
- `aisafe.infrastructure.application/.../ListPilotRosterControllerTest.java`
- `aisafe.infrastructure.application/.../PilotRosterListingTestDoubles.java`
- `aisafe.persistence.impl/.../InMemoryPilotRepository.java`
- `aisafe.persistence.impl/.../JpaPilotRepository.java`

### Modified (support)
- `UserStoryControllerPersistenceSupport.java`
- `AddPilotControllerTest.java`

---

## 9. Observations

- The `AirTransportCompany` aggregate is **not modified**; the association is via `CompanyId` within `Pilot` (NFR08 / DDD).
- `UserId` in the class diagram is a **conceptual reference** from the global model; the US075 implementation embeds `Email`/`FullName` directly in `Pilot`.
- Certified models are resolved via `AircraftModelRepository` and shown as `modelName / manufacturer` in the roster.
