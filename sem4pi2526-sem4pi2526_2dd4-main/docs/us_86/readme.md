# US086 — Pilot remote TCP access

## 1. Context

As a **Pilot**, I want to remotely access the AISafe system via **TCP** to manage flight plans and related operations without using the local EAPLI console.

The RCOMP layer exposes a **line-oriented text protocol** on port **8890**. The server reuses EAPLI application controllers (US080, US082, US043, US111) and does not duplicate business rules.

**Dependencies:** US030 (auth), US075 (pilot profile), US080/082 (flight plan use cases), US043 (weather consultation), US111 (flight plan testing), US044/US078 (architectural pattern).

**Current status:** Implemented (TCP server, client, session handler, optional remote logging, unit tests for unauthenticated paths).

Supporting documentation: [requirements/requisitos.md](requirements/requisitos.md), [design/design.md](design/design.md), [analyse/analise.md](analyse/analise.md), [tests/testes.md](tests/testes.md).

---

## 2. Requirements

**US086** As a Pilot, I want to remotely access the system.

**Acceptance criteria (derived from code and tests):**

| ID | Criterion | Traceability |
|----|-----------|--------------|
| AC-01 | Dedicated TCP server with DB bootstrap and authentication | `PilotServer` |
| AC-02 | Interactive TCP client with login gate and authenticated menu | `PilotClient` |
| AC-03 | Line protocol `COMMAND\|arg1\|arg2\|...` | `PilotSessionHandler.processCommand` |
| AC-04 | Login requires role `PILOT` | `handleLogin` |
| AC-05 | Protected commands require active session | `PilotSessionHandlerTest.pilotCommandsRequireAuthentication` |
| AC-06 | `REGISTER_FLIGHT_PLAN` delegates to EAPLI (US080) | `handleRegisterFlightPlan` |
| AC-07 | `REGISTER_FLIGHT_PLAN_FILE` delegates to EAPLI (US080) | `handleRegisterFlightPlanFile` |
| AC-08 | `INSERT_WEATHER` delegates to EAPLI (US082) | `handleInsertWeather` |
| AC-09 | `CONSULT_WEATHER` delegates to EAPLI (US043) | `handleConsultWeather` |
| AC-10 | `TEST_FLIGHT_PLAN` delegates to EAPLI (US111) | `handleTestFlightPlan` |
| AC-11 | Pilot profile resolved by email linked to `SystemUser` | `resolvePilotId` |
| AC-12 | `PING` works without authentication | `PilotSessionHandlerTest.pingShouldReturnPong` |
| AC-13 | `LOGOUT` returns `BYE` and closes session | `handleLogout` |

**Role:** `PILOT`.

**TCP port:** `8890` (US078 → 8888, US044 → 8889).

---

## 3. Analysis

### 3.1 Actor

| Actor | System role |
|-------|-------------|
| **Pilot** | Authenticated user with role `PILOT` and a linked `Pilot` aggregate (email match) |

### 3.2 Business rules (protocol / session)

- **BR-01:** Protected commands return `ERROR|NOT_AUTHENTICATED` without login.
- **BR-02:** Session is bound to the TCP connection lifetime (no token refresh).
- **BR-03:** `resolvePilotId()` maps `SystemUser` → `PilotId` via `PilotRepository.findByEmail`.
- **BR-04:** EAPLI controllers receive an anonymous `AuthorizationPort` built from the TCP session.
- **BR-05:** File-based commands (`REGISTER_FLIGHT_PLAN_FILE`) require a **server-side path**.

### 3.3 Validations

| # | Component | Check | Where | On failure |
|---|-----------|-------|-------|------------|
| 1 | `PilotSessionHandler` | Role `PILOT` at login | `handleLogin` | `ERROR\|INVALID_CREDENTIALS` |
| 2 | `PilotSessionHandler` | Active session | `ensurePilot()` | `ERROR\|NOT_AUTHENTICATED` / `ERROR\|FORBIDDEN` |
| 3 | `PilotSessionHandler` | Pilot profile linked to user | `resolvePilotId()` | `ERROR\|<message>` |
| 4 | EAPLI controllers | Domain/application rules | per command | `ERROR\|<sanitized message>` |
| 5 | `PilotSessionHandler` | Command field count | per handler | `ERROR\|INVALID_FORMAT` |

---

## 4. Design

### 4.1 Layered architecture

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation (RCOMP client)** | `PilotClient`, `ServerConnection` | Interactive menu; sends TCP commands |
| **Presentation (RCOMP server)** | `PilotClientHandler`, `PilotLoggingClientHandler` | Thread-per-connection I/O loop |
| **Application (RCOMP adapter)** | `PilotSessionHandler` | Parses protocol; maps commands to EAPLI controllers |
| **Application (EAPLI)** | `RegisterFlightPlanController`, etc. | Business use cases (reused) |
| **Infrastructure** | `PersistenceContext`, JPA/H2 | Data access via bootstrap |
| **Infrastructure** | `AuthenticationService` (local per thread) | Login isolation |
| **Infrastructure (optional)** | `RemoteAccessUdpLogger`, `RemoteAccessCommandObserver` | UDP audit events |

### 4.2 Class diagram

Source: [design/cd_us_86.puml](design/cd_us_86.puml)

Regenerate SVG from the repo root (requires PlantUML):

```bash
plantuml docs/us_86/design/cd_us_86.puml docs/us_86/design/sd_us_86.puml
```

### 4.3 Sequence diagram

Source: [design/sd_us_86.puml](design/sd_us_86.puml)

### 4.4 Flow

1. `PilotClient` opens TCP connection to `127.0.0.1:8890`.
2. User authenticates with `LOGIN|username|password`.
3. `PilotSessionHandler` creates local `AuthenticationService`, verifies role `PILOT`, stores `UserSession`.
4. Authenticated menu sends commands; handler resolves `PilotId` and delegates to EAPLI controllers.
5. `LOGOUT` → `BYE`; client calls `ServerConnection.reconnect()` for a new session.

### 4.5 Protocol adapter pattern

`PilotSessionHandler` is a **thin adapter**: it translates pipe-delimited lines into EAPLI controller calls and formats responses. Domain logic stays in `eapli.base`.

---

## 5. Test strategy

### 5.1 Covered scenarios

| Scenario | Test | Layer |
|----------|------|-------|
| **PING without auth** | `pingShouldReturnPong` | RCOMP protocol |
| **Unknown command** | `unknownCommandShouldReturnError` | RCOMP protocol |
| **Login format** | `loginWithInvalidFormatShouldReturnError` | RCOMP protocol |
| **Auth gates** | `pilotCommandsRequireAuthentication` | RCOMP protocol |
| **Logout** | `logoutShouldReturnBye` | RCOMP protocol |

Full detail: [tests/testes.md](tests/testes.md).

### 5.2 Test doubles

| Double | Purpose |
|--------|---------|
| `RemoteAccessUdpLogger("127.0.0.1", 0)` | Disables real UDP in unit tests |

Authenticated flows and EAPLI integration are validated manually (see §7).

---

## 6. Implementation

| Layer | Files | Status |
|-------|-------|--------|
| **RCOMP server** | `PilotServer.java`, `PilotClientHandler.java` | ✅ Done |
| **RCOMP server (logging)** | `PilotServerWithRemoteLogging.java`, `PilotLoggingClientHandler.java` | ✅ Done |
| **RCOMP adapter** | `PilotSessionHandler.java` | ✅ Done |
| **RCOMP client** | `PilotClient.java`, `ServerConnection.java` | ✅ Done |
| **RCOMP logging (local)** | `RemoteAccessUdpLogger.java` (pilot.server) | ✅ Done |
| **EAPLI (reused)** | US080/082/043/111 controllers | ✅ Reused |
| **Tests** | `PilotSessionHandlerTest.java` | ✅ Done |

### 6.1 Package locations

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **RCOMP server** | `aisafe.app.pilot.server` | `eapli.aisafe.app.pilot.server` |
| **RCOMP client** | `aisafe.app.pilot.client` | `eapli.aisafe.app.pilot.client` |
| **EAPLI application** | `aisafe.infrastructure.application` | `eapli.aisafe.flightPlan.application`, `eapli.aisafe.weather.application` |
| **Remote logging (shared)** | `aisafe.app.remote.logging.common` | `eapli.aisafe.app.remote.logging.common` |

---

## 7. Integration / demonstration

### Credentials

| User | Password | Role | Email |
|------|----------|------|-------|
| `pilot1` | `Pilot123!` | PILOT | `pilot@aisafe.com` |

Created by `MasterUsersBootstrapper`. A **`Pilot` aggregate** with matching email must exist (register via EAPLI `AddPilot` or demo data) — otherwise protected commands fail at `resolvePilotId()`.

### Startup

Terminal 1 — server:

```powershell
cd rcomp.base
mvn -q -pl aisafe.app.pilot.server exec:java
```

Terminal 2 — client:

```powershell
cd rcomp.base
mvn -q -pl aisafe.app.pilot.client exec:java
```

Optional remote logging server (UDP 9990 + HTTP 8080):

```powershell
mvn -q -pl aisafe.app.remote.logging.server exec:java
mvn -q -pl aisafe.app.pilot.server exec:java -Dexec.mainClass=eapli.aisafe.app.pilot.server.PilotServerWithRemoteLogging
```

### Client menu

| Option | Action | TCP command |
|--------|--------|-------------|
| 1 | Register flight plan (US080) | `REGISTER_FLIGHT_PLAN\|...` |
| 2 | Register from DSL file (US080) | `REGISTER_FLIGHT_PLAN_FILE\|path\|routeId\|reg` |
| 3 | Insert weather in flight (US082) | `INSERT_WEATHER\|...` |
| 4 | Test flight plan (US111) | `TEST_FLIGHT_PLAN\|planId` |
| 5 | List my flight plans | `LIST_MY_FLIGHT_PLANS` |
| 6 | List testable plans | `LIST_TESTABLE_PLANS` |
| 7 | Consult weather (US043) | `CONSULT_WEATHER\|area\|date` |
| 8 | List routes (helper) | `LIST_ROUTES` |
| 9 | List aircraft (helper) | `LIST_AIRCRAFT` |
| 0 | Logout | `LOGOUT` |

### Tests

```powershell
cd rcomp.base
mvn test -pl aisafe.app.pilot.server -Dtest=PilotSessionHandlerTest
```

---

## 8. Files created

### Documentation
- `docs/us_86/readme.md`
- `docs/us_86/requirements/requisitos.md`
- `docs/us_86/design/design.md`
- `docs/us_86/analyse/analise.md`
- `docs/us_86/tests/testes.md`
- `docs/us_86/design/cd_us_86.puml`
- `docs/us_86/design/sd_us_86.puml`

### Code (US086 — RCOMP)
- `rcomp.base/aisafe.app.pilot.server/.../PilotServer.java`
- `rcomp.base/aisafe.app.pilot.server/.../PilotServerWithRemoteLogging.java`
- `rcomp.base/aisafe.app.pilot.server/.../PilotClientHandler.java`
- `rcomp.base/aisafe.app.pilot.server/.../PilotLoggingClientHandler.java`
- `rcomp.base/aisafe.app.pilot.server/.../PilotSessionHandler.java`
- `rcomp.base/aisafe.app.pilot.server/.../RemoteAccessUdpLogger.java`
- `rcomp.base/aisafe.app.pilot.client/.../PilotClient.java`
- `rcomp.base/aisafe.app.pilot.client/.../ServerConnection.java`
- `rcomp.base/aisafe.app.pilot.server/.../PilotSessionHandlerTest.java`

### EAPLI (reused, not owned by US086)
- `RegisterFlightPlanController`, `RegisterFlightPlanFromFileController` (US080)
- `InsertWeatherDataInFlightController` (US082)
- `ConsultWeatherDataController` (US043)
- `TestFlightPlanController` (US111)

---

## 9. Observations

- Follows the same RCOMP pattern as **US078** (ATCC, 8888) and **US044** (Weather, 8889); command logic is in a dedicated `PilotSessionHandler` (like US044).
- Default `exec-maven` main class is `PilotServer` (no centralized observer logging); use `PilotServerWithRemoteLogging` for UDP audit via `RemoteAccessCommandObserver`.
- `PilotServer` logs login/logout/disconnect **inline** in `PilotSessionHandler` via the local `RemoteAccessUdpLogger`; `PilotLoggingClientHandler` uses the shared module observer and a dummy inline logger (port `0`).
- `TEST_FLIGHT_PLAN` uses `FlightPlanPhysicsValidator(Paths.get("lapr.base"))` — physics validation expects `lapr.base` in the working directory.
- `REGISTER_FLIGHT_PLAN_FILE` and remote import paths must exist on the **server machine**, not the client.
- `ServerConnection.reconnect()` after logout is unique to the pilot client (US078/US044 clients do not reconnect).
