# US076 — Test specification (TDD)

The design was **guided by unit tests written before implementation** (TDD). All tests pass (GREEN phase).

---

## 1. Domain tests — `PilotRosterListingTest`

**Module:** `aisafe.core`  
**Package:** `eapli.aisafe.pilot`

| Method | Scenario | Requirement | AAA |
|--------|----------|-------------|-----|
| `activePilotShouldBeEligibleForRosterListing` | `ACTIVE` pilot eligible | BR-01 / INV-1 | Arrange pilot → Act `isEligibleForRosterListing()` → Assert `true` |
| `inactivePilotShouldNotBeEligibleForRosterListing` | `INACTIVE` excluded | BR-02 | Assert `false` |
| `suspendedPilotShouldNotBeEligibleForRosterListing` | `SUSPENDED` excluded | BR-02 | Assert `false` |
| `retiredPilotShouldNotBeEligibleForRosterListing` | `RETIRED` excluded | BR-02 | Assert `false` |
| `certifiedModelsShouldBeUnmodifiable` | Aggregate encapsulation | INV-2 | Assert `UnsupportedOperationException` |

---

## 2. Application tests — `ListPilotRosterControllerTest`

**Module:** `aisafe.infrastructure.application`  
**Package:** `eapli.aisafe.pilot.application`

| Method | Scenario | Requirement | AAA |
|--------|----------|-------------|-----|
| `defaultConstructorShouldWireDependencies` | `RepositoryFactory` wiring | Infra test | Act `new ListPilotRosterController()` → Assert not null |
| `listShouldReturnActivePilotsSortedByFullName` | **Success** + sorting + DTO mapping + certified model labels | BR-03, BR-04, AC8 | Arrange 2 pilots + model in repo → Act → Assert sorted DTOs with `A320 / Airbus` |
| `listShouldReturnEmptyResultWithMessage` | **Empty list** | BR-07 | Assert `NO_ACTIVE_PILOTS` |
| `listShouldQueryPilotsByAuthenticatedCompany` | `companyId` scope | BR-03 | Assert `lastQueriedCompanyId` |
| `listShouldRelyOnRepositoryToExcludeInactivePilots` | **Inactive exclusion** | BR-01, BR-02 | Assert 1 `ACTIVE` pilot |
| `listShouldFailWhenUserNotLinkedToCompany` | **Authorization/context failure** | BR-05 | Assert `IllegalStateException` |
| `listShouldNotMutatePersistedPilots` | Read-only | BR-06 | Assert `count()` unchanged |

---

## 3. Test doubles — `PilotRosterListingTestDoubles`

| Double | Simulates | Purpose |
|--------|-----------|---------|
| `FakeAuth` | `AuthorizationPort` | Isolate role verification |
| `TestCompanyRepo` | `AirTransportCompanyRepository` | Inject ATCC `CompanyId` |
| `TestPilotRepo` | `PilotRepository` | Control `findActiveByCompanyId` without JPA |
| `TestAircraftModelRepo` | `AircraftModelRepository` | Resolve certified model labels in DTO mapping |

**Isolation (NFR03 / NFR08):** no dependency on `aisafe.persistence.impl`, JPA, or H2 in unit tests.

---

## 4. Execution

```bash
mvn test -pl aisafe.core,aisafe.infrastructure.application \
  -Dtest=PilotRosterListingTest,ListPilotRosterControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

**Current status:** GREEN — all tests passing.
