# US90 — Requirements Engineering

## Actor Identification

| Actor | Description |
|-------|-------------|
| Service Administrator | Human actor responsible for monitoring the operation of the remote access system. Uses the HTTP dashboard to view events and active sessions. |
| (System) Remote Access Logging Server | System actor that receives, stores, and serves access events via UDP and HTTP. |
| (System) Weather TCP Server | System actor (US44) that emits access events for the Weather service. |
| (System) ATC TCP Server | System actor (US78) that emits access events for the ATC service. |
| (System) Pilot TCP Server | System actor (US86) that emits access events for the Pilot service. |

## Use Cases

### UC90.1 — View Recent Events
**Primary Actor:** Service Administrator
**Pre-condition:** Logging server is running
**Post-condition:** Administrator sees a table of recent access events
**Main Success Scenario:**
1. Administrator opens `http://<logging-server>:8080/events` in a browser.
2. The page loads and periodically fetches `/api/events`.
3. The table displays the most recent 500 events with timestamp, event type, user, client IP, port, and service.
4. The table auto-refreshes via AJAX.

### UC90.2 — View Active Sessions
**Primary Actor:** Service Administrator
**Pre-condition:** Logging server is running
**Post-condition:** Administrator sees a table of active (logged-in) sessions
**Main Success Scenario:**
1. Administrator opens `http://<logging-server>:8080/active-users` in a browser.
2. The page loads and periodically fetches `/api/active-users`.
3. The table displays currently logged-in users with client IP, port, service, and connection timestamp.

### UC90.3 — Emit Login Success Event (System)
**Primary Actor:** (System) TCP Server
**Pre-condition:** A client successfully authenticates via the LOGIN command
**Post-condition:** A LOGIN_SUCCESS event is delivered to the logging server
**Main Success Scenario:**
1. TCP Server processes a LOGIN command and receives an "OK" response.
2. `RemoteAccessCommandObserver` detects the successful login.
3. Observer calls `logger.logLoginSuccess(username, clientIp, clientPort)`.
4. Logger sends a UDP datagram to the logging server.
5. Logging server receives, parses, and stores the event.

### UC90.4 — Emit Login Failure Event (System)
**Primary Actor:** (System) TCP Server
**Pre-condition:** A client sends a LOGIN command with invalid credentials
**Post-condition:** A LOGIN_FAILURE event is delivered to the logging server
**Main Success Scenario:**
1. TCP Server processes a LOGIN command and receives an "ERROR|*" response.
2. `RemoteAccessCommandObserver` detects the failed login.
3. Observer calls `logger.logLoginFailure(username, clientIp, clientPort)`.
4. Logger sends a UDP datagram to the logging server.
5. Logging server receives, parses, and stores the event.

### UC90.5 — Emit Logout Event (System)
**Primary Actor:** (System) TCP Server
**Pre-condition:** An authenticated client sends a LOGOUT command
**Post-condition:** A LOGOUT event is delivered to the logging server
**Main Success Scenario:**
1. TCP Server processes a LOGOUT command.
2. `RemoteAccessCommandObserver` detects the logout.
3. Observer calls `logger.logLogout(tracker.loggedInUsername(), clientIp, clientPort)`.
4. Observer calls `tracker.onLogout()`.
5. Logger sends a UDP datagram to the logging server.
6. Logging server receives, parses, and stores the event.

### UC90.6 — Emit Disconnect Event (System)
**Primary Actor:** (System) TCP Server
**Pre-condition:** A TCP connection is closed without an explicit LOGOUT
**Post-condition:** A DISCONNECT event is delivered to the logging server
**Main Success Scenario:**
1. Client connection is closed (e.g. socket error or client crash).
2. The `*LoggingClientHandler`'s `finally` block executes.
3. Handler calls `RemoteAccessCommandObserver.observeDisconnect(...)`.
4. Observer checks `tracker.shouldLogDisconnect()` — returns true.
5. Observer calls `logger.logDisconnect(username, clientIp, clientPort)`.
6. Observer calls `tracker.markDisconnectLogged()`.
7. Logger sends a UDP datagram to the logging server.
8. Logging server receives, parses, and stores the event.

## Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR1 | UDP emission must not block TCP command processing (fire-and-forget) |
| NFR2 | If the logging server is unreachable, each TCP server silently logs the failure to stderr and continues operation |
| NFR3 | Logging host, UDP port, HTTP port, and log file path are configurable via environment variables, system properties, or classpath properties (in priority order) |
| NFR4 | The in-memory event store retains up to 500 events (circular buffer) |
| NFR5 | All events are appended to a log file on disk for persistence |
| NFR6 | The HTTP dashboard must auto-refresh via AJAX without full page reloads |
