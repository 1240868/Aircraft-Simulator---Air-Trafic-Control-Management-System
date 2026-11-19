# US075 — Add a pilot to an air transport company

## 1. User Story

> **As an Air Transport Company Collaborator, I want to add a pilot to my company.**

The collaborator registers a new **pilot** in their air transport company. The pilot becomes a **system user** with role `PILOT` and is certified for one or more registered aircraft models. The pilot is created with `PilotStatus.ACTIVE` and is available for roster listing (US076) and flight plan registration (US080).

---

## 2. Context & Scope

This User Story belongs to the **Pilot Management** bounded context of the AISafe system.

The operation creates two related artefacts:

1. A **`SystemUser`** with role `PILOT` (authentication — US030).
2. A **`Pilot`** aggregate linked to the ATCC's `CompanyId` with certification, license, and certified aircraft models.

The `AirTransportCompany` aggregate is **not modified** — the association is made via `CompanyId` embedded in `Pilot` (fronteira transacional DDD / NFR08).

**Dependencies:** US030 (authentication/authorization), US031 (user management), US055 (aircraft models for certification), US060 (company), US061 (ATCC collaborator linked to company).

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Air Transport Company Collaborator (ATCC)** | Primary actor — registers pilots for their company |
| **System** | Validates input, creates user and pilot, persists the aggregate |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The ATCC must be authenticated in the system |
| PC2 | The ATCC must have the `AIR_TRANSPORT_COMPANY_COLLABORATOR` role |
| PC3 | The ATCC must be linked to an **Air Transport Company** (US061) |
| PC4 | At least one **AircraftModel** must exist in the system (US055) |
| PC5 | The proposed pilot email must not already be registered for another pilot |
| PC6 | The proposed username must not already exist in the system |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | A new `Pilot` aggregate is persisted with `PilotStatus.ACTIVE` |
| PS2 | The pilot is associated with the ATCC's `CompanyId` |
| PS3 | A new `SystemUser` is created with role `PILOT` |
| PS4 | The pilot is certified for at least one existing `AircraftModelId` |
| PS5 | License expiration date is in the future |
| PS6 | No `AirTransportCompany` aggregate is modified |

---

## 6. Main Flow (Basic Course of Events)

1. ATCC selects the option to add a pilot to the company.
2. System verifies authentication and `AIR_TRANSPORT_COMPANY_COLLABORATOR` role.
3. System lists available aircraft models for certification selection.
4. ATCC selects one or more certified model numbers.
5. ATCC provides: username, password, first name, last name, email, certification, license number, license expiration.
6. System validates that each selected aircraft model exists.
7. System verifies the email is not already used by another pilot.
8. System resolves the ATCC's `CompanyId` from the authenticated username.
9. System creates Value Objects (`FullName`, `Email`, `PilotCertification`, `License`).
10. System registers a new `SystemUser` with role `PILOT` via `UserManagementService`.
11. System creates and persists the `Pilot` aggregate with certified models.
12. System confirms successful registration to the ATCC.

---

## 7. Alternative Flows

### AF1 — Collaborator not linked to any company
- At step 8, if `findCompanyIdForCollaboratorUsername` returns empty, the system throws `IllegalStateException` and terminates the use case.

### AF2 — Duplicate pilot email
- At step 7, if `PilotRepository.findByEmail` returns a pilot, the system throws `IntegrityViolationException` and terminates the use case.

### AF3 — No certified models selected
- At step 4, if the list of certified models is empty, the system throws `IllegalArgumentException`.

### AF4 — Unknown aircraft model
- At step 6, if any `AircraftModelId` does not exist, the system throws `IllegalArgumentException`.

### AF5 — Expired license date
- At step 9, if `License` construction fails (expiration in the past), the system throws `IllegalArgumentException`.

### AF6 — Duplicate username
- At step 10, if `UserManagementService.registerNewUser` fails due to duplicate username, the system propagates the framework exception.

### AF7 — User without the required role
- At step 2, if the user does not have `AIR_TRANSPORT_COMPANY_COLLABORATOR`, the system throws `AuthzException`.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Only authenticated users with role **AIR_TRANSPORT_COMPANY_COLLABORATOR** may add pilots |
| AC2 | The pilot is automatically associated with the ATCC's company |
| AC3 | Pilot email must be unique across all pilots |
| AC4 | At least one certified aircraft model is required |
| AC5 | Each certified model must exist in `AircraftModelRepository` |
| AC6 | License number is non-empty; expiration date must be in the future |
| AC7 | Certification text is non-empty |
| AC8 | A `SystemUser` with role `PILOT` is created alongside the pilot |
| AC9 | Initial status is `PilotStatus.ACTIVE` |
| AC10 | Only the `Pilot` aggregate is persisted; `AirTransportCompany` is not modified |
| AC11 | Authentication and authorisation are enforced |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | A pilot belongs to exactly one company, resolved from the authenticated ATCC |
| BR2 | Pilot email is globally unique among pilots |
| BR3 | A pilot must be certified for at least one aircraft model |
| BR4 | Certified models are stored as `@ElementCollection` of `AircraftModelId` (reference by identity) |
| BR5 | License expiration must be strictly in the future at registration time |
| BR6 | The pilot aggregate does not hold a direct reference to `SystemUser` — link is via shared email/username at application level |
| BR7 | Creating a pilot also creates a login-capable user with role `PILOT` |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US030 | Authentication and authorization — cross-cutting prerequisite |
| US031 | User management — `UserManagementService` |
| US055 | Aircraft models — certification targets |
| US060 | Register air transport company — prerequisite |
| US061 | Register ATCC collaborator — prerequisite (links user to company) |
| US076 | List pilot roster — consumes registered pilots |
| US077 | Deactivate pilot — follows US075 |
| US080 | Register flight plan — requires active pilot from same company |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% for domain and controller packages |
| NFR08 | Persistence configurable (in-memory / RDBMS); table `pilots` + `pilot_certified_models` |
| NFR09 | Authentication and authorisation required (`AuthorizationPort`) |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Listing pilots (US076)
- Deactivating pilots (US077)
- Updating pilot certification or license after registration
- Assigning pilots to flight plans (US080)
- Bootstrap of demo pilots (separate bootstrapper if needed)
