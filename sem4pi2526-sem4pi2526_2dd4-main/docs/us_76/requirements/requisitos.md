# US076 — List company pilot roster

## 1. User Story

> **As an Air Transport Company Collaborator, I want to list my company's pilot roster.**

The collaborator views the list of **active** pilots registered in their air transport company, including identification, certification, license, and certified model data. The operation is **read-only** — no aggregate is created or modified.

---

## 2. Context & Scope

This User Story belongs to the **Pilot Management** bounded context of the AISafe system.

The **pilot roster** is the operational view of pilots available to the company. It includes only pilots with `ACTIVE` status; `INACTIVE`, `SUSPENDED`, or `RETIRED` pilots do not appear in the listing.

The query scope is always the authenticated collaborator's **CompanyId** — an ATCC cannot list pilots from another company.

**Dependencies:** US030 (authentication/authorization), US060/061 (company and collaborator), US075 (pilot registration).

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Air Transport Company Collaborator (ATCC)** | Primary actor — requests the roster listing for their company |
| **System** | Authenticates the user, resolves the company, queries active pilots, and returns the result |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The ATCC must be authenticated in the system |
| PC2 | The ATCC must have the `AIR_TRANSPORT_COMPANY_COLLABORATOR` role |
| PC3 | The ATCC must be linked to an **Air Transport Company** (US061) |
| PC4 | The persistence infrastructure (`PilotRepository`) must be configured |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | The system returns a list of `ACTIVE` pilots belonging to the ATCC's company |
| PS2 | The list is sorted by **full name** in ascending order |
| PS3 | No `Pilot` or `AirTransportCompany` aggregate is modified |
| PS4 | If no active pilots exist, the system returns an empty list with the `NO_ACTIVE_PILOTS` message |

---

## 6. Main Flow (Basic Course of Events)

1. ATCC selects the "List company pilot roster" option.
2. System verifies authentication and `AIR_TRANSPORT_COMPANY_COLLABORATOR` role.
3. System obtains the authenticated user's `username`.
4. System resolves the `CompanyId` linked to the collaborator via `AirTransportCompanyRepository`.
5. System invokes `PilotRepository.findActiveByCompanyId(companyId)`.
6. System maps each `Pilot` to `PilotRosterListItemDTO` via `PilotRosterListing.toRow(pilot, modelRepo)`, resolving certified aircraft model labels through `AircraftModelRepository`.
7. System sorts results by `fullName`.
8. System returns `PilotRosterListResult` to the presentation layer.
9. System displays the roster to the ATCC (or an empty-list message).

---

## 7. Alternative Flows

### AF1 — Collaborator not linked to any company
- At step 4, if `findCompanyIdForCollaboratorUsername` returns empty, the system throws `IllegalStateException` and terminates the use case.

### AF2 — No active pilots in the company
- At step 5, if the repository returns an empty list, the system returns `PilotRosterListResult` with empty `pilots` and `messageWhenEmpty = NO_ACTIVE_PILOTS`.

### AF3 — User without the required role
- At step 2, if the user does not have `AIR_TRANSPORT_COMPANY_COLLABORATOR`, the system throws `AuthzException` and terminates the use case.

### AF4 — Inactive, suspended, or retired pilots
- At step 5, pilots with `PilotStatus` other than `ACTIVE` are excluded by the `findActiveByCompanyId` contract (also validated by `Pilot.isEligibleForRosterListing()` in the domain).

---

## 8. Acceptance Criteria

| ID | Criterion | Traceability test |
|----|-----------|-------------------|
| AC1 | Only pilots with `PilotStatus.ACTIVE` appear in the roster | `PilotRosterListingTest.activePilotShouldBeEligibleForRosterListing` |
| AC2 | `INACTIVE`, `SUSPENDED`, and `RETIRED` pilots are excluded | `inactive/suspended/retiredPilotShouldNotBeEligible...` |
| AC3 | Listing is restricted to the authenticated ATCC's `CompanyId` | `ListPilotRosterControllerTest.listShouldQueryPilotsByAuthenticatedCompany` |
| AC4 | User must have `AIR_TRANSPORT_COMPANY_COLLABORATOR` role | `ListPilotRosterControllerTest.listShouldReturnActivePilotsSortedByFullName` |
| AC5 | ATCC not linked to a company → failure with `IllegalStateException` | `ListPilotRosterControllerTest.listShouldFailWhenUserNotLinkedToCompany` |
| AC6 | Empty list returns `NO_ACTIVE_PILOTS` message | `ListPilotRosterControllerTest.listShouldReturnEmptyResultWithMessage` |
| AC7 | Results sorted by full name in ascending order | `ListPilotRosterControllerTest.listShouldReturnActivePilotsSortedByFullName` |
| AC8 | Each roster row includes: `pilotId`, `fullName`, `email`, `certification`, `licenseNumber`, `licenseExpiration`, `status`, `certifiedModels` (model name / manufacturer labels) | `ListPilotRosterControllerTest.listShouldReturnActivePilotsSortedByFullName` |
| AC9 | Operation does not alter persisted data | `ListPilotRosterControllerTest.listShouldNotMutatePersistedPilots` |
| AC10 | The aggregate's certified models list is immutable | `PilotRosterListingTest.certifiedModelsShouldBeUnmodifiable` |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | A pilot is eligible for the roster only if `Pilot.isEligibleForRosterListing()` returns `true` (`ACTIVE` status) |
| BR2 | `INACTIVE`, `SUSPENDED`, and `RETIRED` states never appear in the roster |
| BR3 | The listing query uses exclusively the authenticated collaborator's `CompanyId` |
| BR4 | The `Pilot` aggregate references `CompanyId` by identity — it does not modify `AirTransportCompany` |
| BR5 | Presentation mapping is done via DTOs; the `Pilot` aggregate is not exposed to the UI |
| BR6 | The listing is sorted by `fullName` before being returned to the consumer |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US030 | Authentication and authorization — cross-cutting prerequisite |
| US060 | Register air transport company — prerequisite |
| US061 | Register ATCC collaborator — prerequisite (links user to company) |
| US075 | Add pilot — prerequisite (creates pilots queryable in the roster) |
| US077 | Deactivate pilot — changes eligibility (pilot no longer appears in roster) |
| US080 | Register flight plan — consumes pilot lists in related contexts |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% in domain and controller packages |
| NFR08 | Configurable persistence (in-memory / RDBMS); manual test doubles for isolation |
| NFR09 | Authentication and authorization required (`AuthorizationPort`) |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Creating, editing, or removing pilots (US075 / US077)
- Changing pilot status during listing
- Listing pilots from other companies
- Pagination, advanced filters, or data export
- Web graphical interface (console UI implemented; remote TCP via US78 in `rcomp.base`)
