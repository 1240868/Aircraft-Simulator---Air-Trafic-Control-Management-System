# US90 — Analysis

## Domain Model Overview

The remote access logging system involves three layers:

1. **Common Library** (`aisafe.app.remote.logging.common`) — shared abstractions for UDP emission, datagram parsing, event representation, session tracking, and configuration.
2. **TCP Servers** (per service) — `*LoggingClientHandler` implementations that intercept TCP command/response pairs and emit UDP events via the common library.
3. **Centralized Logging Server** (`aisafe.app.remote.logging.server`) — dedicated node that receives UDP datagrams, stores events in memory and on disk, and serves an HTTP monitoring dashboard.

## Key Domain Concepts

| Concept | Description |
|---------|-------------|
| RemoteAccessServiceId | Enumeration of the three TCP services: WEATHER, ATC, PILOT |
| RemoteAccessLogEntry | Immutable value object representing a single remote access event |
| RemoteAccessUdpLogger | Service that marshals events into UDP datagrams and sends them to the logging server |
| RemoteAccessLogStore | Central thread-safe repository of events and derived active sessions |
| ActiveSession | Immutable value object representing a currently logged-in session |
| RemoteAccessSessionTracker | Per-connection state machine that tracks authentication status for disconnect detection |
| RemoteAccessDatagramParser | Utility that validates and parses raw UDP payloads into RemoteAccessLogEntry instances |
| RemoteAccessCommandObserver | Static observer that inspects TCP command/response pairs and triggers the appropriate log method |

## Attribute Tables

### RemoteAccessLogEntry
| Field | Type | Description |
|-------|------|-------------|
| eventType | String | One of: LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, DISCONNECT |
| timestamp | java.time.Instant | ISO-8601 instant when the event was generated |
| username | String | The authenticated username, or "-" for failures / unknown |
| clientIp | String | Remote client IPv4 address |
| clientPort | int | Remote client TCP port number |
| serviceId | String | Service code: WEATHER, ATC, or PILOT |

### ActiveSession
| Field | Type | Description |
|-------|------|-------------|
| username | String | Authenticated username |
| clientIp | String | Client IPv4 address |
| clientPort | int | Client TCP port |
| serviceId | String | Service code |
| since | java.time.Instant | Timestamp of the LOGIN_SUCCESS event |

### RemoteAccessSessionTracker
| Field | Type | Description |
|-------|------|-------------|
| loggedInUsername | String (nullable) | Currently authenticated username, null if no active session |
| disconnectLogged | boolean | Whether a DISCONNECT event has already been emitted for the current session |

## Protocol Definition

UDP payload format (pipe-delimited UTF-8 text, single datagram per event):

```
eventType|timestamp|username|clientIp|clientPort|serviceId
```

- **eventType**: One of `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `DISCONNECT`
- **timestamp**: ISO-8601 instant (e.g. `2026-06-14T16:00:00Z`)
- **username**: Client-supplied username or `-` if blank
- **clientIp**: Dotted-decimal IPv4 address
- **clientPort**: Integer TCP port
- **serviceId**: One of `WEATHER`, `ATC`, `PILOT`

**Examples:**

```
LOGIN_SUCCESS|2026-06-14T16:00:00Z|pilot1|192.168.1.10|54321|PILOT
LOGIN_FAILURE|2026-06-14T16:00:01Z|attacker|10.0.0.5|12345|ATC
LOGOUT|2026-06-14T18:30:00Z|atcuser|192.168.1.20|9876|ATC
DISCONNECT|2026-06-14T19:00:00Z|weather1|192.168.1.30|4567|WEATHER
```

## Glossary

| Term | Definition |
|------|------------|
| Active Session | A TCP connection where the client has successfully authenticated and not yet logged out or disconnected |
| Logging Server | Dedicated server node that receives UDP events and serves the HTTP monitoring dashboard |
| Remote Access | TCP-based access to one of the three system services (Weather, ATC, Pilot) from a remote client |
| TCP Server | One of the three service-specific servers (Weather, ATC, Pilot) that accept remote client TCP connections |
| Service Administrator | Human role responsible for monitoring system operation |
