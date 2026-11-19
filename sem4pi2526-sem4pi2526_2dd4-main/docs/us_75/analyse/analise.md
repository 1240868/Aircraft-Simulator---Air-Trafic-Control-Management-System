# US075 — Analysis

## 1. Overview

Domain analysis for **US075 — Add a pilot to an air transport company**. The ATCC registers a new pilot in their company, creating both a domain aggregate and a framework `SystemUser` with role `PILOT`.

Related documentation: [requirements/requisitos.md](../requirements/requisitos.md), [sequence-diagrams/readme.md](../Design/sequence-diagrams/readme.md), [class-diagrams/readme.md](../Design/class-diagrams/readme.md).

Diagrams: [sequence-diagrams/sd_us_75.puml](../Design/sequence-diagrams/sd_us_75.puml), [class-diagrams/US075-class-diagram.puml](../Design/class-diagrams/US075-class-diagram.puml).

**Status:** implemented.

---

## 2. Actor

| Actor | Description |
|-------|-------------|
| **Air Transport Company Collaborator (ATCC)** | Authenticated collaborator with role `AIR_TRANSPORT_COMPANY_COLLABORATOR`, linked to one air transport company |

---

## 3. Domain concepts

| Concept | Type | Justification |
|---------|------|---------------|
| `Pilot` | **Aggregate Root** | Central entity; owns pilot identity, status, and certification data |
| `PilotId` | **Value Object** | Unique aggregate identity (UUID) |
| `PilotStatus` | **Enum** | Lifecycle (`ACTIVE` on creation; `INACTIVE` via US077) |
| `FullName` | **Value Object** | First and last name; validated at construction |
| `Email` | **Value Object** | Pilot contact; uniqueness checked at application level |
| `PilotCertification` | **Value Object** | Certification label (e.g. ATPL, CPL); non-empty |
| `License` | **Value Object** | License number + expiration; rejects past dates |
| `CompanyId` | **Value Object (ref)** | Company association — reference by identity |
| `AircraftModelId` | **Value Object (ref)** | Certified models; validated read-only against `AircraftModelRepository` |
| `AircraftModel` | **Aggregate Root (external)** | Consulted but never modified |

---

## 4. Aggregate design — Pilot

```
Pilot (Aggregate Root)
├── PilotId
├── FullName, Email
├── CompanyId              ← reference by identity (does not modify AirTransportCompany)
├── PilotCertification, License
├── PilotStatus            ← ACTIVE on creation
└── certifiedModels        → List<AircraftModelId> (@ElementCollection)
```

### Invariants

| ID | Invariant | Where enforced |
|----|-----------|----------------|
| INV-1 | Email unique among pilots | `AddPilotController` → `PilotRepository.findByEmail` |
| INV-2 | At least one certified model | `AddPilotController.addPilot` |
| INV-3 | Each certified model must exist | `AircraftModelRepository.ofIdentity` |
| INV-4 | License expiration in the future | `License` constructor |
| INV-5 | Certification non-empty | `PilotCertification` constructor |
| INV-6 | Company = ATCC's company | `resolveCompanyOfAuthenticatedUser()` |
| INV-7 | Initial status ACTIVE | `Pilot` constructor |

### Cross-aggregate references

```
Pilot ──(companyId)──────────► AirTransportCompany   (read-only ref)
Pilot ──(certifiedModels)────► AircraftModel          (read-only refs)
```

> **Fronteira transacional:** apenas `Pilot` é criado/persistido. `AirTransportCompany` e `AircraftModel` não são alterados.

---

## 5. Application-level rules

| # | Check | Where | On failure |
|---|-------|-------|------------|
| 1 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `AuthorizationPort` | `AuthzException` |
| 2 | ATCC linked to a `CompanyId` | `resolveCompanyOfAuthenticatedUser()` | `IllegalStateException` |
| 3 | Email not duplicate | `PilotRepository.findByEmail` | `IntegrityViolationException` |
| 4 | Certified models non-empty | `addPilot()` | `IllegalArgumentException` |
| 5 | Each model exists | `AircraftModelRepository.ofIdentity` | `IllegalArgumentException` |
| 6 | Create `SystemUser` with `PILOT` role | `UserManagementService.registerNewUser` | Framework exception |
| 7 | Persist new `Pilot` | `PilotRepository.save` | — |

---

## 6. Repository interfaces

### PilotRepository (extended for US075)

```java
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {
    Optional<Pilot> findByEmail(Email email);
}
```

### Supporting repositories (pre-existing)

- `AirTransportCompanyRepository` — `findCompanyIdForCollaboratorUsername`
- `AircraftModelRepository` — `findAll`, `ofIdentity`

---

## 7. Layer responsibilities

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `AddPilotUI` | Lists models; collects user/pilot data; parses model selections |
| **Application** | `AddPilotController` | Authorization, validation, user registration, pilot creation |
| **Application** | `UserManagementService` (framework) | Creates `SystemUser` with role `PILOT` |
| **Domain** | `Pilot`, VOs | Invariants at construction |
| **Infrastructure** | `PilotRepository`, JPA/InMemory | Persistence (`pilots`, `pilot_certified_models`) |
| **Infrastructure** | `AuthorizationPort` | Role check (US030) |

---

## 8. Key design decisions

| Decision | Justification |
|----------|---------------|
| Pilot references company by `CompanyId` | Preserves aggregate boundary; no modification to `AirTransportCompany` |
| Dual creation: `SystemUser` + `Pilot` | Pilot must be able to log in (US080); orchestrated at application layer |
| Certified models as `@ElementCollection` | Simple persistence; models referenced by ID only |
| Email uniqueness at application level | Cross-aggregate rule; not natural to a single VO |
| `UserManagementService` before `Pilot.save` | User must exist before pilot can authenticate; order matters on failure handling |

---

## 9. Functional dependencies

| US | Relationship |
|----|-------------|
| US055 | Aircraft models for certification |
| US060/061 | Company and ATCC must exist |
| US076 | Lists pilots created by US075 |
| US077 | Deactivates pilots created by US075 |
| US080 | Consumes pilots for flight plan registration |
| US030 | Authentication and authorization |
