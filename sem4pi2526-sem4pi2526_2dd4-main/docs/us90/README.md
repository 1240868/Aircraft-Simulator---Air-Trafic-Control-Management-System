# US90 - Remote Access Logging

## Identification
- **US Number:** US90
- **Title:** Remote Access Logging
- **Actor:** Service Administrator (indirect, human)
- **Component:** RCOMP (Remote Communication)
- **Module:** `rcomp.base`

## Description
As a Service Administrator, I want the system to have a remote access logging capability so that the operation of the system can be monitored remotely.

The solution provides a centralized logging server that receives UDP datagrams containing access events (login, logout, disconnect) from the three remote TCP servers (Weather, ATC, Pilot). Events are stored in-memory and persisted to a file, and a built-in HTTP dashboard allows browsing recent events and active sessions in real-time.

## Acceptance Criteria
1. Every LOGIN (success/failure), LOGOUT, and unexpected TCP DISCONNECT on the Weather, ATC, and Pilot servers must emit a UDP datagram to the remote logging server.
2. Failed login attempts must be logged separately from successful ones.
3. The logging server must provide an HTTP dashboard with:
   - Recent access events table (time, event type, user, client IP, client port, service).
   - Currently active sessions table (user, client IP, client port, service, connected since).
4. The UDP protocol uses pipe-delimited UTF-8 text payloads.
5. Logging server configuration (host, UDP port, HTTP port) is externalized via properties files, system properties, or environment variables.

## Dependencies
- **US44** — Weather Remote Access (TCP server on port 8889)
- **US78** — ATC Remote Access (TCP server on port 8888)
- **US86** — Pilot Remote Access (TCP server on port 8890)
- The shared `aisafe.app.remote.logging.common` library

## Notes
- The logging server is a standalone Java application (`RemoteAccessLoggingServer`) that runs on a dedicated node.
- Each TCP server has a dedicated `*LoggingClientHandler` that wraps the original session handler and emits UDP events via `RemoteAccessCommandObserver`.
- The HTTP dashboard is non-secure and intended for local/monitoring network use only.
