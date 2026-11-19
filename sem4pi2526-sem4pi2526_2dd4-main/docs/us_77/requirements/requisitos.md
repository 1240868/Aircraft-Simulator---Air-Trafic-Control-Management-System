# US077 — Remove (deactivate) a pilot

## 1. User Story

> **As an Air Transport Company Collaborator, I want to remove a pilot from my company's roster.**

The collaborator deactivates an **active** pilot registered in their air transport company. Deactivation sets `PilotStatus` to `INACTIVE`; the pilot no longer appears in the roster (US076). The pilot record is **not deleted** — only the status changes.

---

## 2. Context & Scope

This User Story belongs to the **Pilot Management** bounded context of the AISafe system.

**Remove** in this context means **deactivate** (`ACTIVE` → `INACTIVE`), not physical deletion of the aggregate.

**Critical invariant:** a pilot with **assigned flight plans** cannot be deactivated. The application queries `FlightPlanRepository` and the domain enforces the rule before changing status.

The operation scope is always the authenticated collaborator's **CompanyId** — an ATCC cannot deactivate pilots from another company.

**Dependencies:** US030 (authentication/authorization), US060/061 (company and collaborator), US075 (pilot registration), US080 (flight plans may assign pilots).

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Air Transport Company Collaborator (ATCC)** | Primary actor — requests deactivation of a pilot from their company roster |
| **System** | Authenticates the user, resolves the company, validates flight-plan assignments, deactivates the pilot, and persists the change |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The ATCC must be authenticated in the system |
| PC2 | The ATCC must have the `AIR_TRANSPORT_COMPANY_COLLABORATOR` role |
| PC3 | The ATCC must be linked to an **Air Transport Company** (US061) |
| PC4 | The target pilot must exist and belong to the ATCC's company |
| PC5 | The target pilot must be in `ACTIVE` status |
| PC6 | The pilot must have **no assigned flight plans** |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | The pilot's status is updated to `INACTIVE` |
| PS2 | The updated `Pilot` aggregate is persisted |
| PS3 | The pilot no longer appears in the active roster (US076) |
| PS4 | No `FlightPlan` or `AirTransportCompany` aggregate is modified |
| PS5 | The deactivated pilot cannot be assigned to new flight plans (US080 / `FlightPlanFactory.pilotIsActive`) |

---

## 6. Main Flow (Basic Course of Events)

1. ATCC selects the option to remove/deactivate a pilot.
2. System verifies authentication and `AIR_TRANSPORT_COMPANY_COLLABORATOR` role.
3. System obtains the authenticated user's `username`.
4. System resolves the `CompanyId` linked to the collaborator.
5. ATCC provides the target `PilotId`.
6. System loads the pilot via `PilotRepository.ofIdentity(pilotId)`.
7. System verifies the pilot belongs to the collaborator's `CompanyId`.
8. System queries `FlightPlanRepository.hasAssignedFlightPlansFor(pilotId)`.
9. System invokes `Pilot.deactivate(hasAssignedFlightPlans)`.
10. System persists the pilot via `PilotRepository.save(pilot)`.
11. System confirms successful deactivation to the ATCC.

---

## 7. Alternative Flows

### AF1 — Collaborator not linked to any company
- At step 4, if no company is found, the system throws `IllegalStateException` and terminates the use case.

### AF2 — Pilot not found
- At step 6, if the pilot does not exist, the system throws `IllegalArgumentException` and terminates the use case.

### AF3 — Pilot belongs to another company
- At step 7, if `pilot.getCompany()` differs from the ATCC's `CompanyId`, the system throws `IllegalArgumentException` and terminates the use case.

### AF4 — Pilot has assigned flight plans
- At step 9, if `hasAssignedFlightPlans` is `true`, the domain throws `IllegalStateException` and the pilot remains `ACTIVE`.

### AF5 — Pilot is not ACTIVE
- At step 9, if the pilot is `INACTIVE`, `SUSPENDED`, or `RETIRED`, the domain throws `IllegalStateException` and no change is persisted.

### AF6 — User without the required role
- At step 2, if the user does not have `AIR_TRANSPORT_COMPANY_COLLABORATOR`, the system throws `AuthzException` and terminates the use case.

---

## 8. Acceptance Criteria

| ID | Criterion | Traceability test |
|----|-----------|-------------------|
| AC1 | Only `ACTIVE` pilots can be deactivated | `DeactivatePilotTest.deactivateShouldRejectWhenPilotIsNotActive` |
| AC2 | Deactivation sets status to `INACTIVE` when no flight plans are assigned | `DeactivatePilotTest.deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans` |
| AC3 | Pilot with assigned flight plans cannot be deactivated | `DeactivatePilotTest.deactivateShouldRejectWhenPilotHasAssignedFlightPlans` |
| AC4 | Already `INACTIVE` pilot cannot be deactivated again | `DeactivatePilotTest.deactivateShouldRejectWhenPilotAlreadyInactive` |
| AC5 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` required | `DeactivatePilotControllerTest.deactivateShouldSetPilotInactiveAndPersist` |
| AC6 | Deactivation restricted to the ATCC's company | `DeactivatePilotControllerTest.deactivateShouldFailWhenPilotBelongsToAnotherCompany` |
| AC7 | ATCC not linked to a company → failure | `DeactivatePilotControllerTest.deactivateShouldFailWhenUserNotLinkedToCompany` |
| AC8 | Pilot not found → failure | `DeactivatePilotControllerTest.deactivateShouldFailWhenPilotNotFound` |
| AC9 | Assigned flight plans queried before deactivation | `DeactivatePilotControllerTest.deactivateShouldQueryAssignedFlightPlansForPilot` |
| AC10 | Successful deactivation persists the pilot | `DeactivatePilotControllerTest.deactivateShouldSetPilotInactiveAndPersist` |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | Deactivation means `PilotStatus.ACTIVE` → `INACTIVE` (soft remove) |
| BR2 | A pilot with assigned flight plans must never be deactivated |
| BR3 | Only `ACTIVE` pilots are eligible for deactivation |
| BR4 | The operation uses exclusively the authenticated collaborator's `CompanyId` |
| BR5 | The `Pilot` aggregate references `CompanyId` by identity — it does not modify `AirTransportCompany` |
| BR6 | `FlightPlan` assignment is checked via `FlightPlanRepository` (cross-aggregate reference by `PilotId`) |
| BR7 | After deactivation, the pilot is excluded from the US076 roster (`isEligibleForRosterListing() == false`) |
| BR8 | Inactive pilots are excluded from `RegisterFlightPlanController.listPilots()` and blocked by `FlightPlanFactory.pilotIsActive` when registering plans |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US030 | Authentication and authorization — cross-cutting prerequisite |
| US060/061 | Company and ATCC — prerequisite |
| US075 | Add pilot — prerequisite (creates pilots to deactivate) |
| US076 | List pilot roster — deactivated pilots no longer appear |
| US080 | Register flight plan — may assign pilots (blocks deactivation) |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% in domain and controller packages |
| NFR08 | Configurable persistence; manual test doubles for isolation |
| NFR09 | Authentication and authorization required (`AuthorizationPort`) |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Physical deletion of pilot records
- Deactivating pilots from other companies
- Modifying or cancelling flight plans during deactivation
- Bulk deactivation of multiple pilots
- Remote TCP access (covered by US78 in `rcomp.base`; console UI is implemented)
