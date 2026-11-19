# US086 — Analysis

## 1. Overview

Domain and protocol analysis for **US086 — Pilot remote TCP access**. The RCOMP layer exposes EAPLI flight-plan and weather use cases over a line-oriented TCP protocol on port **8890**.

Related documentation: [requirements/requisitos.md](../requirements/requisitos.md), [design/design.md](../design/design.md).  
Diagrams: [design/cd_us_86.puml](../design/cd_us_86.puml), [design/sd_us_86.puml](../design/sd_us_86.puml).

**Status:** implemented.

---

## 2. Actor

| Actor | Description |
|-------|-------------|
| **Pilot** | Authenticated user with role `PILOT` and a `Pilot` profile linked by email |

---

## 3. Domain concepts

| Concept | Type | Justification |
|---------|------|---------------|
| `Pilot` | **Aggregate Root** (EAPLI) | Resolved via email for all protected operations |
| `FlightPlan` | **Aggregate Root** (EAPLI) | Registered, listed, tested remotely |
| `FlightLeg` | **Entity** (EAPLI) | Target for weather insertion (US082) |
| `UserSession` | **Infrastructure** | Per-TCP-connection auth state |
| `PilotSessionHandler` | **Adapter** | Maps TCP commands to EAPLI use cases |
| `AuthorizationPort` | **Port** | Bridges TCP session to EAPLI security |

---

## 4. Session model

```
TCP Connection (1:1)
├── UserSession currentSession
├── loggedInUsername (for UDP audit)
└── PilotSessionHandler
      ├── processCommand(line)
      ├── resolvePilotId() → PilotId
      └── authorizationPort() → AuthorizationPort adapter
```

### Session rules

| ID | Rule | Where |
|----|------|-------|
| SR-1 | Login creates local `AuthenticationService` (not global registry session) | `handleLogin` |
| SR-2 | `PILOT` role required at login and for protected commands | `handleLogin`, `ensurePilot` |
| SR-3 | `PilotId` from `PilotRepository.findByEmail(user.email())` | `resolvePilotId` |
| SR-4 | Logout clears session and returns `BYE` | `handleLogout` |
| SR-5 | Abrupt disconnect triggers `notifyDisconnect()` if user was logged in | `PilotClientHandler.finally` |

---

## 5. Command → EAPLI mapping

| TCP command | EAPLI component | User Story |
|-------------|-----------------|------------|
| `REGISTER_FLIGHT_PLAN` | `RegisterFlightPlanController` | US080 |
| `REGISTER_FLIGHT_PLAN_FILE` | `RegisterFlightPlanFromFileController` | US080 |
| `INSERT_WEATHER` | `InsertWeatherDataInFlightController` | US082 |
| `CONSULT_WEATHER` | `ConsultWeatherDataController` | US043 |
| `TEST_FLIGHT_PLAN` | `TestFlightPlanController` | US111 |
| `LIST_ROUTES` | `RegisterFlightPlanController.listFlightRoutes()` | helper |
| `LIST_AIRCRAFT` | `RegisterFlightPlanController.listAircraft()` | helper |
| `LIST_MY_FLIGHT_PLANS` | `PersistenceContext.repositories().flightPlans()` | helper |
| `LIST_TESTABLE_PLANS` | `TestFlightPlanController.listTestablePlansForCurrentPilot()` | helper |

---

## 6. Application-level rules (RCOMP adapter)

| # | Check | Where | On failure |
|---|-------|-------|------------|
| 1 | Command recognized | `processCommand` switch | `ERROR\|UNKNOWN_COMMAND` |
| 2 | Login field count ≥ 3 | `handleLogin` | `ERROR\|INVALID_FORMAT` |
| 3 | Role `PILOT` at login | `AuthenticationService.authenticate` | `ERROR\|INVALID_CREDENTIALS` |
| 4 | Session for protected commands | `ensurePilot()` | `ERROR\|NOT_AUTHENTICATED` |
| 5 | Pilot profile exists | `resolvePilotId()` | `ERROR\|No pilot profile linked...` |
| 6 | Field counts per command | each `handle*` | `ERROR\|INVALID_FORMAT` |
| 7 | Response sanitization | `sanitize()` / `sanitizeMultiline()` | No `\|` or `\n` in single-line errors |

---

## 7. Comparison with sibling RCOMP user stories

| Aspect | US078 (ATCC) | US044 (Weather) | US086 (Pilot) |
|--------|--------------|-----------------|---------------|
| Port | 8888 | 8889 | **8890** |
| Role | `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `WEATHER_PERSON` | **`PILOT`** |
| Session handler | Logic in `ClientHandler` | `WeatherSessionHandler` | **`PilotSessionHandler`** |
| EAPLI US exposed | US076 (`LIST_PILOTS`) | US041–043 | **US080, US082, US043, US111** |
| Inline UDP logging | No (observer only) | No (observer only) | **Yes in default `PilotServer`** |
| Client reconnect | No | No | **Yes after logout** |

---

## 8. Risks and mitigations

| Risk | Impact | Mitigation in code |
|------|--------|-------------------|
| No `Pilot` profile for `pilot1` user | All protected commands fail | Document prerequisite; register pilot via EAPLI |
| `lapr.base` missing | Physics test fails | Document server working directory |
| Server-side file paths | Client cannot upload DSL | Document path must exist on server |
| Double UDP logging in logging variant | Duplicate audit events | Dummy logger (port 0) + observer pattern |
| Unbounded threads per connection | Resource exhaustion under load | Acceptable for academic/demo scope |
