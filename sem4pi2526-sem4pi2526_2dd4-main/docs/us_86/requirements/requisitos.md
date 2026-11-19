# US086 — Pilot remote TCP access

## 1. User Story

> **As a Pilot, I want to remotely access the system.**

The pilot connects via TCP to execute flight-plan and weather-related operations already implemented in the EAPLI application layer, without using the local backoffice console.

---

## 2. Context & Scope

This User Story belongs to the **RCOMP** bounded context of AISafe — remote access over TCP.

The pilot uses a **dedicated TCP server** (port **8890**) and an interactive **console client**. The server bootstraps the EAPLI persistence layer and delegates all business logic to existing application controllers.

**Dependencies:**

| US | Relationship |
|----|--------------|
| US030 | Authentication and authorization |
| US075 | Pilot aggregate (profile linked by email) |
| US080 | Register flight plan (manual + DSL file) |
| US082 | Insert weather data in flight plan |
| US043 | Consult weather data |
| US111 | Test / validate flight plan (DSL + physics) |
| US044 / US078 | Architectural pattern (remote TCP gateway) |

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Pilot** | Primary actor — authenticates with role `PILOT` and executes remote commands |
| **System (RCOMP server)** | Accepts TCP connections, maintains per-connection session, delegates to EAPLI |
| **System (RCOMP client)** | Provides interactive menu and sends protocol commands |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | EAPLI database bootstrapped (`AisafeBootstrapper`) |
| PC2 | A `SystemUser` with role `PILOT` exists |
| PC3 | A `Pilot` aggregate exists with **email matching** the system user (for protected commands) |
| PC4 | TCP server listening on port **8890** |
| PC5 | For `TEST_FLIGHT_PLAN`: `lapr.base` available relative to server working directory (physics validator) |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | On successful login, the server holds a `UserSession` for the TCP connection |
| PS2 | Flight plan / weather operations persist through EAPLI repositories (when commands succeed) |
| PS3 | On `LOGOUT` or connection close, the session is cleared |
| PS4 | Optional: UDP audit events sent to Remote Access Logging Server (logging variants) |

---

## 6. Main Flow (Basic Course of Events)

1. Pilot starts `PilotClient` and connects to `127.0.0.1:8890`.
2. Pilot selects Login and provides username/password.
3. Server authenticates via local `AuthenticationService` with role `PILOT`.
4. Server responds `OK`; client shows authenticated menu.
5. Pilot selects an operation (e.g. register flight plan).
6. Client sends the corresponding TCP command line.
7. `PilotSessionHandler` verifies session, resolves `PilotId`, calls the EAPLI controller.
8. Server returns `OK|...` or `ERROR|...` (possibly multi-line).
9. Client displays the result.
10. Pilot selects Logout → server returns `BYE`; client reconnects for a new session.

---

## 7. Alternative Flows

### AF1 — Invalid credentials
- At step 3, authentication fails → `ERROR|INVALID_CREDENTIALS`; optional UDP `LOGIN_FAILURE`.

### AF2 — Not authenticated
- At step 7, no active session → `ERROR|NOT_AUTHENTICATED`.

### AF3 — No pilot profile linked
- At step 7, `resolvePilotId()` finds no `Pilot` for the user email → `ERROR|No pilot profile linked to user: ...`.

### AF4 — Invalid command format
- Missing fields → `ERROR|INVALID_FORMAT` (per command minimum field count).

### AF5 — Domain / application failure
- EAPLI controller throws → `ERROR|<sanitized message>` (pipes and newlines removed from message).

### AF6 — DSL validation failure (file register)
- `RegisterFlightPlanFromFileController` fails → `ERROR|DSL_VALIDATION_FAILED|<details>` (may span multiple lines).

### AF7 — Flight plan test failure
- Test completes but validations fail → `OK|FAILED|<status>` followed by `DSL|...` and/or `PHYSICS|...` lines.

### AF8 — Connection closed without logout
- Handler calls `notifyDisconnect()` → optional UDP `DISCONNECT` event.

---

## 8. Acceptance Criteria

| ID | Criterion | Traceability |
|----|-----------|--------------|
| AC1 | TCP server on port 8890 with bootstrap and accept loop | `PilotServer.main` |
| AC2 | TCP client with login gate and authenticated menu | `PilotClient` |
| AC3 | Protocol: one command per line, fields separated by `\|` | `PilotSessionHandler.processCommand` |
| AC4 | Login requires role `PILOT` | `handleLogin` |
| AC5 | Protected commands require authentication | `PilotSessionHandlerTest.pilotCommandsRequireAuthentication` |
| AC6 | `PING` returns `PONG` without authentication | `PilotSessionHandlerTest.pingShouldReturnPong` |
| AC7 | `LOGOUT` returns `BYE` | `PilotSessionHandlerTest.logoutShouldReturnBye` |
| AC8 | `REGISTER_FLIGHT_PLAN` exposes US080 | `handleRegisterFlightPlan` |
| AC9 | `REGISTER_FLIGHT_PLAN_FILE` exposes US080 (DSL) | `handleRegisterFlightPlanFile` |
| AC10 | `INSERT_WEATHER` exposes US082 | `handleInsertWeather` |
| AC11 | `CONSULT_WEATHER` exposes US043 | `handleConsultWeather` |
| AC12 | `TEST_FLIGHT_PLAN` exposes US111 | `handleTestFlightPlan` |
| AC13 | Helper commands `LIST_ROUTES`, `LIST_AIRCRAFT` support registration | handlers in `PilotSessionHandler` |
| AC14 | `LIST_MY_FLIGHT_PLANS`, `LIST_TESTABLE_PLANS` support pilot workflow | handlers in `PilotSessionHandler` |
| AC15 | Pilot identity resolved from authenticated user email | `resolvePilotId` |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | One `UserSession` per TCP connection; no cross-thread session sharing |
| BR2 | Login uses a **local** `AuthenticationService` instance (reflection on `AuthorizationService`) |
| BR3 | Protected commands require `currentSession` with role `PILOT` |
| BR4 | `PilotId` is resolved via `PilotRepository.findByEmail` matching `SystemUser` email or username |
| BR5 | EAPLI controllers receive `AuthorizationPort` adapter from TCP session — domain rules unchanged |
| BR6 | Multi-line responses use `OK\|<count>` header; `LEG\|` continuation lines excluded from client count |
| BR7 | File paths in commands refer to the **server filesystem** |

---

## 10. Related User Stories

| US | Relationship |
|----|--------------|
| US030 | Auth prerequisite |
| US075 | Pilot profile prerequisite |
| US080 | Register flight plan (exposed remotely) |
| US082 | Insert weather in flight (exposed remotely) |
| US043 | Consult weather (exposed remotely) |
| US111 | Test flight plan (exposed remotely) |
| US078 | ATCC remote access — same RCOMP pattern (port 8888) |
| US044 | Weather remote access — same RCOMP pattern (port 8889) |
| US091 | HTTP dashboard for remote access audit (optional) |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR09 | Authentication and role check required for protected commands |
| NFR10 | Implementation language: Java 17 |
| — | Thread-per-connection TCP model (blocking I/O) |
| — | UTF-8 line protocol; no TLS in current implementation |

---

## 12. Out of Scope for this US

- Implementing flight plan / weather business rules (owned by EAPLI US080/082/043/111)
- Web or graphical UI
- File upload over TCP (only server-side paths)
- TLS / encryption
- Pilot registration or profile management (US075)
- ATCC or Weather Person remote access (US078 / US044)
