# US077 — Test specification (TDD)

The design was **guided by unit tests written before implementation** (TDD). All tests pass (GREEN phase).

---

## 1. Domain tests — `DeactivatePilotTest`

**Module:** `aisafe.core`  
**Package:** `eapli.aisafe.pilot`

| Method | Scenario | Requirement | AAA |
|--------|----------|-------------|-----|
| `deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans` | `ACTIVE` → `INACTIVE` | BR1 / INV-1 | Arrange pilot → Act `deactivate(false)` → Assert `INACTIVE` |
| `deactivateShouldRejectWhenPilotHasAssignedFlightPlans` | **Critical invariant** | BR2 / INV-1 | Act `deactivate(true)` → Assert `IllegalStateException` + remains `ACTIVE` |
| `deactivateShouldRejectWhenPilotIsNotActive` | `SUSPENDED` blocked | BR3 / INV-2 | Assert `IllegalStateException` |
| `deactivateShouldRejectWhenPilotAlreadyInactive` | Double deactivation blocked | INV-2 | Assert `IllegalStateException` |

---

## 2. Application tests — `DeactivatePilotControllerTest`

**Module:** `aisafe.infrastructure.application`  
**Package:** `eapli.aisafe.pilot.application`

| Method | Scenario | Requirement | AAA |
|--------|----------|-------------|-----|
| `defaultConstructorShouldWireDependencies` | Wiring `RepositoryFactory` | Infra test | Act `new DeactivatePilotController()` → Assert not null |
| `deactivateShouldSetPilotInactiveAndPersist` | **Success** + auth + persist | AC5, AC10 | Arrange pilot → Act → Assert `INACTIVE` + `saveCount == 1` |
| `deactivateShouldQueryAssignedFlightPlansForPilot` | Orchestration: flight plan check | AC9 | Assert `lastCheckedPilotId` |
| `deactivateShouldFailWhenPilotHasAssignedFlightPlans` | **Flight plan guard** | BR2 / AC3 | Assert exception + no save |
| `deactivateShouldFailWhenPilotBelongsToAnotherCompany` | Company scope | BR4 / AC6 | Assert `IllegalArgumentException` |
| `deactivateShouldFailWhenPilotNotFound` | Unknown pilot | AC8 | Assert `IllegalArgumentException` |
| `deactivateShouldFailWhenUserNotLinkedToCompany` | Context failure | AC7 | Assert `IllegalStateException` |

---

## 3. Related tests — flight plan assignment guard (US080)

**Module:** `aisafe.infrastructure.application`  
**Package:** `eapli.aisafe.flightPlan.application`

| Method | Scenario | Requirement |
|--------|----------|-------------|
| `listPilotsShouldReturnOnlyActivePilots` | Inactive pilots excluded from selection list | BR8 |
| `shouldThrowWhenPilotInactive` | Registration blocked for inactive pilot | BR8 |

---

## 4. Test doubles — `PilotRosterListingTestDoubles`

| Double | Simulates | Purpose |
|--------|-----------|---------|
| `FakeAuth` | `AuthorizationPort` | Isolate role verification |
| `TestCompanyRepo` | `AirTransportCompanyRepository` | Inject ATCC `CompanyId` |
| `TestPilotRepo` | `PilotRepository` | In-memory store + `saveCount` / `lastSavedPilot` |
| `TestFlightPlanRepo` | `FlightPlanRepository` | Control `hasAssignedFlightPlansFor` without JPA |

**Isolation (NFR03 / NFR08):** no dependency on `aisafe.persistence.impl`, JPA, or H2 in unit tests.

---

## 5. Execution

```bash
mvn test -pl aisafe.core,aisafe.infrastructure.application \
  -Dtest=DeactivatePilotTest,DeactivatePilotControllerTest,RegisterFlightPlanControllerTest#listPilotsShouldReturnOnlyActivePilots,RegisterFlightPlanControllerTest#shouldThrowWhenPilotInactive \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Current status:** GREEN — all tests passing.
