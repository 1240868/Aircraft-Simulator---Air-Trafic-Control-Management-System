# US086 — Design

## 1. Overview

This document describes the detailed software design for **US086 — Pilot remote TCP access**, following the **RCOMP gateway pattern** established by US078 and US044.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_86/design/`.

Diagrams: [cd_us_86.puml](cd_us_86.puml), [sd_us_86.puml](sd_us_86.puml).

**Current status:** implemented; unit tests cover unauthenticated protocol paths.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│           RCOMP CLIENT (Presentation)                │
│     PilotClient + ServerConnection (TCP 8890)        │
└───────────────────────┬─────────────────────────────┘
                        │ line protocol
┌───────────────────────▼─────────────────────────────┐
│           RCOMP SERVER (Presentation)                │
│  PilotServer → PilotClientHandler (thread/conn)      │
│              → PilotSessionHandler (adapter)         │
└───────────────────────┬─────────────────────────────┘
                        │ delegates
┌───────────────────────▼─────────────────────────────┐
│         EAPLI APPLICATION (reused)                   │
│  RegisterFlightPlanController        (US080)        │
│  RegisterFlightPlanFromFileController (US080)       │
│  InsertWeatherDataInFlightController  (US082)        │
│  ConsultWeatherDataController       (US043)        │
│  TestFlightPlanController             (US111)       │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│         INFRASTRUCTURE                               │
│  PersistenceContext (JPA/H2)                        │
│  AuthenticationService (local per connection)       │
│  AuthorizationPort (anonymous adapter)              │
└─────────────────────────────────────────────────────┘
```

### Package mapping

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **RCOMP client** | `aisafe.app.pilot.client` | `eapli.aisafe.app.pilot.client` |
| **RCOMP server** | `aisafe.app.pilot.server` | `eapli.aisafe.app.pilot.server` |
| **EAPLI application** | `aisafe.infrastructure.application` | `eapli.aisafe.flightPlan.application`, `eapli.aisafe.weather.application` |
| **Remote logging** | `aisafe.app.remote.logging.common` | `eapli.aisafe.app.remote.logging.common` |

---

## 3. Class Design

### 3.1 RCOMP Server — `aisafe.app.pilot.server`

#### `PilotServer`

```java
public final class PilotServer {
    private static final int PORT = 8890;

    public static void main(final String[] args) {
        AuthzRegistry.configure(...);
        new AisafeBootstrapper().execute();
        try (var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket clientSocket = serverSocket.accept();
                new Thread(new PilotClientHandler(clientSocket)).start();
            }
        }
    }
}
```

#### `PilotClientHandler`

```java
final class PilotClientHandler implements Runnable {
    private final PilotSessionHandler sessionHandler;

    PilotClientHandler(final Socket socket) {
        this.sessionHandler = new PilotSessionHandler(
                socket.getInetAddress().getHostAddress(),
                socket.getPort(),
                new RemoteAccessUdpLogger());
    }

    public void run() {
        // readLine → sessionHandler.processCommand → println response
        // on close: sessionHandler.notifyDisconnect()
    }
}
```

#### `PilotSessionHandler` — Protocol adapter

```java
final class PilotSessionHandler {
    String processCommand(final String line) {
        return switch (cmd) {
            case "LOGIN" -> handleLogin(parts);
            case "LOGOUT" -> handleLogout();
            case "PING" -> "PONG";
            case "LIST_ROUTES" -> handleListRoutes();
            case "LIST_AIRCRAFT" -> handleListAircraft();
            case "LIST_MY_FLIGHT_PLANS" -> handleListMyFlightPlans();
            case "LIST_TESTABLE_PLANS" -> handleListTestablePlans();
            case "REGISTER_FLIGHT_PLAN" -> handleRegisterFlightPlan(parts);
            case "REGISTER_FLIGHT_PLAN_FILE" -> handleRegisterFlightPlanFile(parts);
            case "INSERT_WEATHER" -> handleInsertWeather(parts);
            case "CONSULT_WEATHER" -> handleConsultWeather(parts);
            case "TEST_FLIGHT_PLAN" -> handleTestFlightPlan(parts);
            default -> "ERROR|UNKNOWN_COMMAND";
        };
    }
}
```

**Key private methods:**

| Method | EAPLI controller | US |
|--------|------------------|-----|
| `handleRegisterFlightPlan` | `RegisterFlightPlanController.register(...)` | US080 |
| `handleRegisterFlightPlanFile` | `RegisterFlightPlanFromFileController.registerFromFile(...)` | US080 |
| `handleInsertWeather` | `InsertWeatherDataInFlightController.insertWeatherData(...)` | US082 |
| `handleConsultWeather` | `ConsultWeatherDataController.consult(...)` | US043 |
| `handleTestFlightPlan` | `TestFlightPlanController.testForCurrentPilot(...)` | US111 |
| `resolvePilotId` | `PilotRepository.findByEmail` | US075 |

#### `PilotLoggingClientHandler` (optional variant)

Uses `RemoteAccessCommandObserver` from `remote.logging.common` and passes a **dummy** inline logger (`port 0`) to `PilotSessionHandler` to avoid duplicate UDP emission on the default code path while still allowing `notifyDisconnect()` from the session handler.

---

### 3.2 RCOMP Client — `aisafe.app.pilot.client`

#### `PilotClient`

- Authentication submenu (Login / Ping / Exit).
- Authenticated menu mapping options 1–9 to TCP commands.
- On logout: `LOGOUT` → `ServerConnection.reconnect()`.

#### `ServerConnection`

```java
String send(final String command) throws IOException {
    out.println(command);
    final String firstLine = in.readLine();
    // Multi-line: OK|count (+ LEG| lines excluded from count)
    // TEST: OK|PASSED| / OK|FAILED| + DSL| / PHYSICS| lines
    // FILE: ERROR|DSL_VALIDATION_FAILED| + continuation lines
}
```

---

## 4. Protocol specification

Separator: `|` (pipe). One message per line. UTF-8.

### 4.1 LOGIN

```
Request:  LOGIN|<username>|<password>
Response: OK
          ERROR|INVALID_FORMAT
          ERROR|INVALID_CREDENTIALS
          ERROR|AUTH_FAILED
```

### 4.2 LOGOUT

```
Request:  LOGOUT
Response: BYE
```

### 4.3 PING

```
Request:  PING
Response: PONG
```

(No authentication required.)

### 4.4 REGISTER_FLIGHT_PLAN (US080)

```
Request:  REGISTER_FLIGHT_PLAN|<routeUuid>|<aircraftReg>|<designator>|<flightType>|<yyyy-MM-dd>|<HH:mm>
Response: OK|<planUuid>|<status>
          ERROR|NOT_AUTHENTICATED | ERROR|INVALID_FORMAT | ERROR|INVALID_DATETIME | ERROR|<msg>
```

Departure datetime: `date + "T" + time` → `LocalDateTime`.

### 4.5 REGISTER_FLIGHT_PLAN_FILE (US080)

```
Request:  REGISTER_FLIGHT_PLAN_FILE|<serverPath>|<routeUuid>|<aircraftReg>
Response: OK|<planUuid>|<designator>|<status>
          ERROR|FILE_READ_FAILED|<msg>
          ERROR|DSL_VALIDATION_FAILED|<details>  (multi-line possible)
```

### 4.6 INSERT_WEATHER (US082)

```
Request:  INSERT_WEATHER|<planUuid>|<legUuid>|<areaCode>|<observedAt>|<tempC>|<humidity>|<windMs>|<windDir>
Response: OK
          ERROR|INVALID_NUMBER | ERROR|INVALID_DATE | ERROR|<msg>
```

`observedAt`: ISO-8601; if no `Z` or offset, server appends `Z`.

### 4.7 CONSULT_WEATHER (US043)

```
Request:  CONSULT_WEATHER|<areaCode>|<yyyy-MM-dd>
Response: OK|0
          OK|<count>
          <id>|<area>|<observedAt>|<temp>|<hum>|<windMs>|<windDir>|<source>  (× count)
```

### 4.8 TEST_FLIGHT_PLAN (US111)

```
Request:  TEST_FLIGHT_PLAN|<planUuid>
Response: OK|PASSED|<newStatus>
          OK|FAILED|<newStatus>
          DSL|<error>       (× n)
          PHYSICS|<error>   (× m)
          ERROR|<msg>
```

### 4.9 LIST commands

```
LIST_ROUTES
  → OK|<count>\n<routeId>|<name>|<origin>|<dest>

LIST_AIRCRAFT
  → OK|<count>\n<aircraftId>|<modelId>|<status>

LIST_MY_FLIGHT_PLANS
  → OK|<count>\n<planId>|<designator>|<status>|<routeId>
     LEG|<planId>|<legId>|<order>   (continuation, not counted in client header)

LIST_TESTABLE_PLANS
  → OK|<count>\n<planId>|<designator>|<status>
```

---

## 5. Sequence Summary (textual — SD in separate artefact)

```
Pilot → PilotClient: start / login
PilotClient → ServerConnection: send("LOGIN|pilot1|Pilot123!")
ServerConnection → PilotClientHandler: TCP line
PilotClientHandler → PilotSessionHandler: processCommand
PilotSessionHandler → AuthenticationService: authenticate(..., PILOT)
PilotSessionHandler ← UserSession
PilotSessionHandler → PilotClientHandler: "OK"

Pilot → PilotClient: register flight plan
PilotClient → ServerConnection: REGISTER_FLIGHT_PLAN|...
PilotSessionHandler → resolvePilotId()
PilotSessionHandler → RegisterFlightPlanController.register(...)
Controller → PersistenceContext: save FlightPlan
PilotSessionHandler → PilotClient: OK|<id>|<status>
```

---

## 6. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Thin client / thick server** | `PilotClient` / `PilotSessionHandler` | Business logic on server |
| **Adapter** | `PilotSessionHandler`, `AuthorizationPort` anonymous impl | TCP ↔ EAPLI |
| **Thread-per-connection** | `PilotServer` accept loop | Simple concurrent model |
| **Reuse** | EAPLI controllers unchanged | Single source of business rules |
| **Decorator (optional)** | `PilotLoggingClientHandler` + `RemoteAccessCommandObserver` | Audit without changing session handler |
| **Fire-and-forget UDP** | `RemoteAccessUdpLogger` | Non-blocking remote access audit |

---

## 7. Error Handling

| Response | Trigger | Layer |
|----------|---------|-------|
| `ERROR\|NOT_AUTHENTICATED` | Protected command without session | RCOMP adapter |
| `ERROR\|FORBIDDEN` | Session without `PILOT` role | RCOMP adapter |
| `ERROR\|INVALID_CREDENTIALS` | Failed login | RCOMP adapter |
| `ERROR\|UNKNOWN_COMMAND` | Unrecognized command | RCOMP adapter |
| `ERROR\|INVALID_FORMAT` | Insufficient fields | RCOMP adapter |
| `ERROR\|<msg>` | Domain/application exception (sanitized) | EAPLI via adapter |

---

## 8. Test Contract

Traceability detail: [tests/testes.md](../tests/testes.md).

| Scenario | Test class |
|----------|------------|
| PING without auth | `PilotSessionHandlerTest` |
| Unknown command | `PilotSessionHandlerTest` |
| Login format / auth gates | `PilotSessionHandlerTest` |
| Logout BYE | `PilotSessionHandlerTest` |

Authenticated and EAPLI integration flows: manual demonstration (§7 in readme).
