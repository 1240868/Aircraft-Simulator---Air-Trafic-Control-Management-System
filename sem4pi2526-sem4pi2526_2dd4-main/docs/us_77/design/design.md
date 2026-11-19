# US077 — Design

## 1. Overview

This document describes the detailed software design for **US077 — Remove (deactivate) a pilot**, following a **Layered Architecture** with **DDD tactical patterns**.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_77/design/`.

Diagrams: [cd_us_77.puml](cd_us_77.puml), [sd_us_77.puml](sd_us_77.puml).

**Current status:** implemented and covered by unit tests (domain, application, persistence, console UI).

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│         DeactivatePilotUI (Console)                │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│                 APPLICATION LAYER                    │
│  DeactivatePilotController                         │
│  PilotRosterListing (reuse from US076)             │
└──────────┬────────────────────────┬─────────────────┘
           │ uses domain objects    │ calls
┌──────────▼────────────┐  ┌───────▼─────────────────┐
│     DOMAIN LAYER      │  │   REPOSITORY INTERFACES  │
│  Pilot (AR)           │  │  PilotRepository         │
│  PilotStatus          │  │  FlightPlanRepository    │
└───────────────────────┘  │  AirTransportCompany     │
                           │    Repository            │
                           └───────┬─────────────────┘
                                   │ implemented by
                           ┌───────▼─────────────────┐
                           │  INFRASTRUCTURE LAYER    │
                           │  Jpa/InMemory repos      │
                           │  AuthorizationPort       │
                           └─────────────────────────┘
```

### Package mapping

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **core.domain** | `aisafe.core` | `eapli.aisafe.pilot`, `eapli.aisafe.flightPlan.repositories` |
| **application** | `aisafe.infrastructure.application` | `eapli.aisafe.pilot.application` |
| **infrastructure.persistence** | `aisafe.persistence.impl` | `eapli.aisafe.persistence.impl.jpa` / `.inmemory` |

---

## 3. Class Design

### 3.1 Domain Layer — `aisafe.core`

#### `Pilot` — Aggregate Root (extension)

```java
@Entity
public class Pilot implements AggregateRoot<PilotId> {

    private PilotStatus status;  // ACTIVE by default (US075)

    /** US077 — deactivates when no flight plans are assigned. */
    public void deactivate(final boolean hasAssignedFlightPlans) {
        if (status != PilotStatus.ACTIVE) {
            throw new IllegalStateException(
                "Only active pilots can be deactivated");
        }
        if (hasAssignedFlightPlans) {
            throw new IllegalStateException(
                "Pilot has assigned flight plans and cannot be deactivated");
        }
        this.status = PilotStatus.INACTIVE;
    }
}
```

**US077 invariants (tested in `DeactivatePilotTest`):**

| ID | Invariant |
|----|-----------|
| INV-1 | `ACTIVE` + no assigned plans → `INACTIVE` |
| INV-1 | Assigned flight plans → `IllegalStateException`, status unchanged |
| INV-2 | Non-`ACTIVE` status → `IllegalStateException` |

**Cross-aggregate rule:** flight-plan assignment is queried by the application layer; the domain receives a `boolean` flag to enforce the invariant without coupling to `FlightPlan`.

---

### 3.2 Repository Interfaces — Domain Layer

```java
public interface FlightPlanRepository extends DomainRepository<FlightPlanId, FlightPlan> {

    /** US077 — true when the pilot has at least one assigned flight plan. */
    boolean hasAssignedFlightPlansFor(PilotId pilotId);
}
```

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    // ofIdentity, save — used by DeactivatePilotController
}
```

---

### 3.3 Application Layer — `aisafe.infrastructure.application`

#### `DeactivatePilotController`

```java
@UseCaseController
public class DeactivatePilotController {

    private final AuthorizationPort authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final FlightPlanRepository flightPlanRepo;

    public void deactivatePilot(final PilotId pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        final String username = authz.currentUsername();
        final CompanyId companyId =
            PilotRosterListing.requireCompanyOfUser(companyRepo, username);

        final Pilot pilot = pilotRepo.ofIdentity(pilotId)
            .orElseThrow(() -> new IllegalArgumentException("Pilot not found"));

        if (!pilot.getCompany().equals(companyId)) {
            throw new IllegalArgumentException(
                "Pilot does not belong to the authenticated company");
        }

        final boolean hasPlans =
            flightPlanRepo.hasAssignedFlightPlansFor(pilotId);

        pilot.deactivate(hasPlans);
        pilotRepo.save(pilot);
    }
}
```

**Reused helper:** `PilotRosterListing.requireCompanyOfUser` (US076) resolves the ATCC's `CompanyId`.

**Presentation:** `DeactivatePilotUI` lists active pilots via `ListPilotRosterController`, then calls `deactivatePilot(pilotId)`.

**Downstream effect (US080):** deactivated pilots are excluded from `RegisterFlightPlanController.listPilots()` and rejected by `FlightPlanFactory.pilotIsActive`.

---

### 3.4 Infrastructure Layer — `aisafe.persistence.impl`

```java
// InMemoryFlightPlanRepository
@Override
public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
    return matchOne(fp -> fp.getPilot() != null && fp.getPilot().equals(pilotId)).isPresent();
}

// JpaFlightPlanRepository
@Override
public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
    return matchOne("e.pilot.value = :pilotId", "pilotId", pilotId.toString()).isPresent();
}
```

Implementations: `InMemoryFlightPlanRepository`, `JpaFlightPlanRepository`.

> **Note:** for JPA deployment, `FlightPlan` must be registered in `persistence.xml` so assignments are persisted and queryable.

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
ATCC → UI: menu "Remove pilot"
UI → Controller: deactivatePilot(pilotId)
Controller → AuthorizationPort: ensureAuthenticatedUserHasAnyOf(ATCC)
Controller → AuthorizationPort: currentUsername()
Controller → PilotRosterListing: requireCompanyOfUser(companyRepo, username)
  → AirTransportCompanyRepository: findCompanyIdForCollaboratorUsername
Controller → PilotRepository: ofIdentity(pilotId)
Controller → verify pilot.company == companyId
Controller → FlightPlanRepository: hasAssignedFlightPlansFor(pilotId)
Controller → Pilot: deactivate(hasPlans)
Controller → PilotRepository: save(pilot)
Controller ← success
UI ← confirmation
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository** | `PilotRepository`, `FlightPlanRepository` | Abstracts persistence; cross-aggregate query |
| **Aggregate Root** | `Pilot.deactivate()` | Encapsulates status transition invariants |
| **Reference by identity** | `CompanyId`, `PilotId` on `FlightPlan` query | DDD cross-aggregate rule without direct coupling |
| **Use Case Controller** | `@UseCaseController` on `DeactivatePilotController` | EAPLI pattern for use cases |
| **Helper reuse** | `PilotRosterListing.requireCompanyOfUser` | Shared company-resolution logic (US076) |
| **Test Double (manual)** | `FakeAuth`, `TestPilotRepo`, `TestFlightPlanRepo` | Full DB isolation in tests (NFR08) |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `AuthzException` | User without `AIR_TRANSPORT_COMPANY_COLLABORATOR` role | Application |
| `IllegalStateException` | ATCC not linked to any company | Application |
| `IllegalArgumentException` | Pilot not found | Application |
| `IllegalArgumentException` | Pilot belongs to another company | Application |
| `IllegalStateException` | Assigned flight plans / non-ACTIVE pilot | Domain |

---

## 7. TDD Contract

The design was **guided by unit tests written before implementation**. Traceability detail: [tests/testes.md](../tests/testes.md).

| Scenario | Test class |
|----------|------------|
| Status transition `ACTIVE` → `INACTIVE` | `DeactivatePilotTest` |
| Block deactivation with assigned flight plans | `DeactivatePilotTest` + `DeactivatePilotControllerTest` |
| Only `ACTIVE` eligible | `DeactivatePilotTest` |
| Company scope + authorization | `DeactivatePilotControllerTest` |
| Orchestration (find → check → deactivate → save) | `DeactivatePilotControllerTest` |
