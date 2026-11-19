# US073 — Create a Flight Route

## 1. User Story

> **As an Air Transport Company Collaborator, I want to add a flight route for my company.**

A route connects two airports (origin and destination) and has a name composed of **2 letters** (the company's initials) followed by **up to 4 digits** (e.g. `TP123`). The route name must be **unique** in the system.

---

## 2. Context & Scope

This User Story belongs to the **Flight Management** bounded context. A **FlightRoute** is the static operational template that links two airports under a given air transport company. It is a prerequisite for creating flight plans (US080) and scheduling flights.

The route is created by an **Air Transport Company Collaborator (ATCC)** on behalf of their company. Origin and destination must be registered, operational airports.

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Air Transport Company Collaborator** | Primary actor — creates routes for their company |
| **System** | Validates input, computes route characteristics, persists the route |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The ATCC must be authenticated and authorised in the system |
| PC2 | The ATCC must be linked to an **Air Transport Company** (US061) |
| PC3 | Both **origin** and **destination airports** must exist and be operational (US052) |
| PC4 | The proposed route name must not already exist in the system |
| PC5 | Origin and destination airports must be different |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | A new `FlightRoute` aggregate is persisted and associated with the ATCC's company |
| PS2 | The route name follows the format `xxn(n)(n)(n)` (2 letters + 1–4 digits) |
| PS3 | Origin and destination airport references are stored |
| PS4 | Estimated **distance** and **duration** are calculated from airport coordinates |
| PS5 | The route is created in **active** status |

---

## 6. Main Flow (Basic Course of Events)

1. ATCC selects the option to create a new flight route.
2. System presents available operational airports.
3. ATCC provides: route numeric suffix (1–4 digits), origin airport and destination airport.
4. System derives the route name prefix from the company's IATA code (2 letters).
5. System validates the complete route name format and uniqueness.
6. System loads and validates both airports (existence, operational status).
7. System verifies origin ≠ destination.
8. System calculates distance and duration from airport coordinates.
9. System creates and persists the `FlightRoute` aggregate.
10. System confirms successful creation to the ATCC.

---

## 7. Alternative Flows

### AF1 — Invalid route name format
- At step 5, if the name does not match `^[A-Z]{2}\d{1,4}$`, the system presents an error and returns to step 3.

### AF2 — Duplicate route name
- At step 5, if the route name already exists globally, the system presents an error and returns to step 3.

### AF3 — Unknown or inactive airport
- At step 6, if either airport does not exist or is not operational, the system presents an error and returns to step 3.

### AF4 — Origin equals destination
- At step 7, if origin and destination are the same airport, the system rejects the route and returns to step 3.

### AF5 — ATCC not linked to a company
- At step 1, if the authenticated user is not associated with any company, the system denies the operation.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Only authenticated users with role **AIR_TRANSPORT_COMPANY_COLLABORATOR** may create routes |
| AC2 | The route is automatically associated with the ATCC's company |
| AC3 | Route name = company IATA code (2 letters) + numeric suffix (1–4 digits), e.g. `TP123`, `TP1234` |
| AC4 | Route name must be globally unique |
| AC5 | Origin and destination must be different registered airports |
| AC6 | Both airports must be operational at creation time |
| AC7 | Distance and duration are derived from airport coordinates |
| AC8 | The route is persisted with active status |
| AC9 | Authentication and authorisation are enforced |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | Route name format: exactly 2 uppercase letters followed by 1 to 4 digits |
| BR2 | The 2-letter prefix corresponds to the company's IATA designator |
| BR3 | Route names are unique across the entire system, not just within a company |
| BR4 | Origin airport must differ from destination airport |
| BR5 | A route references airports by identity (`AirportId`) only |
| BR6 | Distance is computed using geographic coordinates (e.g. Haversine formula) |
| BR7 | Duration is estimated from distance and a standard cruise speed assumption |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US052 | Create an airport — prerequisite |
| US060 | Register air transport company — prerequisite |
| US061 | Add customer's collaborator — ATCC must exist |
| US074 | Delete/deactivate flight route — follows US073 |
| US080 | Create flight plan — consumes routes |
| US030 | Authentication and authorisation — cross-cutting prerequisite |
| US78 | ATCC remote access — future remote availability |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% for domain and controller packages |
| NFR08 | Persistence must be configurable (in-memory / relational RDBMS) |
| NFR09 | Authentication and authorisation must be enforced |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Route deactivation (US074)
- Flight plan creation (US080)
- Multi-stop routes
- Route editing after creation
- Aircraft compatibility validation at route creation time (may be added as enhancement)
