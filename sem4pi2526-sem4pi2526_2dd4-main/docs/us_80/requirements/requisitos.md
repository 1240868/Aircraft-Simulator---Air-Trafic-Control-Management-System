# US080 — Register a Flight Plan for a Route

## 1. User Story

> **As a Pilot, I want to register a flight plan for a route.**

---

## 2. Context & Scope

This User Story is part of the **Flight Management** module of the AISafe system.
A flight plan is the formal record that links a specific route to an actual execution instance, including the aircraft, crew, fuel load and departure date/time.

The flight plan starts with the status **"draft"**.

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Pilot** | Primary actor — initiates and owns the flight plan |
| **System** | Validates input, persists the flight plan |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The Pilot must be authenticated and authorised in the system with role `PILOT` |
| PC2 | A valid **FlightRoute** must exist (created by an Air Transport Company Collaborator) |
| PC3 | The FlightRoute must be **active** (not deactivated) |
| PC4 | A compatible **Aircraft** must exist in the company's fleet and be **active** |
| PC5 | A **Pilot** must exist in the system and be **active** |
| PC6 | The Pilot must belong to the **same company** as the route |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | A new FlightPlan is persisted with status **"draft"** |
| PS2 | The FlightPlan is associated with the given FlightRoute, Aircraft, Pilot, designator, flight type and departure date/time |

---

## 6. Main Flow (Basic Course of Events)

1. Pilot selects the option to register a new flight plan ("Register Flight Plan").
2. System presents a list of active pilots in the system.
3. Pilot selects a pilot from the list.
4. Based on the selected pilot's company, system presents a list of active routes belonging to that company.
5. Pilot selects a route.
6. Based on the selected pilot's company, system presents a list of active aircraft belonging to that company.
7. Pilot selects an aircraft.
8. System prompts for the flight designator, flight type and departure date/time.
9. Pilot provides the required data.
10. System validates all inputs (designator format, route status, aircraft status, pilot status, company match, fuel capacity).
11. System creates the flight plan with status **"draft"**.
12. System confirms successful registration to the Pilot.

---

## 7. Alternative Flows

### AF1 — No pilots available
- If no pilot records exist, the system notifies the Pilot and terminates.

### AF2 — No active routes for the selected pilot's company
- If no active routes exist, the system notifies the user and returns to step 2.

### AF3 — No active aircraft for the selected pilot's company
- If no active aircraft exist, the system notifies the user and returns to step 2.

### AF4 — Invalid designator format
- If the designator does not match `^[A-Z]{2}\d{4}$`, the system presents an error and returns to step 8.

### AF5 — Invalid departure date/time
- If the departure date/time cannot be parsed (`yyyy-MM-dd HH:mm`), the system presents an error and returns to step 8.

### AF6 — Route not found, inactive, or company mismatch
- If validation fails, the system presents an error and returns to step 2.

### AF7 — Aircraft not found or inactive
- If validation fails, the system presents an error and returns to step 2.

### AF8 — Fuel exceeds aircraft model capacity
- If the total fuel (from all legs) exceeds the aircraft model's max fuel capacity, the system presents an error.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | The flight plan must reference an existing, active FlightRoute |
| AC2 | The aircraft assigned must be active |
| AC3 | The Pilot must be active |
| AC4 | The Pilot must belong to the same company as the route |
| AC5 | The flight plan is created with status **"draft"** |
| AC6 | The flight designator must match format `^[A-Z]{2}\d{4}$` |
| AC7 | The first 2 letters of the designator must match the company IATA code |
| AC8 | Total fuel must not exceed the aircraft model's max fuel capacity |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | A flight designator is composed of 2 uppercase letters (matching the company IATA code) + exactly 4 digits: `^[A-Z]{2}\d{4}$` |
| BR2 | It is not possible to create a flight plan for a deactivated aircraft |
| BR3 | It is not possible to create a flight plan on a deactivated route |
| BR4 | Only Pilots belonging to the route's company may register flight plans on that route |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US073 | Create flight route — prerequisite |
| US070 | Add aircraft to fleet — prerequisite |
| US075 | Add pilot — prerequisite |
| US081 | Create flight plan from file — alternative creation method |
| US082 | Insert weather data in a flight — extends US080 |
| US085 | Test/validate flight plan — follows US080 |
| US030 | Authentication and authorisation — cross-cutting prerequisite |

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

- Flight physics calculations (US085 / SCOMP)
- DSL parsing and validation (US081 / LPROG)
- Weather data integration (US082)
- Air traffic simulation (US100+)
