# US90 — Design

## Architecture Overview

The solution follows a **centralized logging server** pattern with fire-and-forget UDP emission from three distributed TCP servers.

```
┌──────────────────────┐      UDP       ┌──────────────────────────┐
│  Pilot TCP Server    │───────────────▶│                          │
│  (port 8890)         │                │    Remote Access         │
│  PilotLogging*       │                │    Logging Server        │
├──────────────────────┤                │    (UDP :9990)           │
│  ATC TCP Server      │───────────────▶│    HTTP :8080            │
│  (port 8888)         │                │                          │
│  AtcLogging*         │                ├──────────────────────────┤
├──────────────────────┤                │  ┌────────────────────┐  │
│  Weather TCP Server  │───────────────▶│  │  RemoteAccessLogStore │
│  (port 8889)         │                │  └────────────────────┘  │
│  LoggingClientHandler│                │  ┌────────────────────┐  │
└──────────────────────┘                │  │  remote-access-    │  │
                                        │  │  events.log (file) │  │
                                        │  └────────────────────┘  │
                                        └──────────────────────────┘
```

## Key Design Patterns

| Pattern | Usage |
|---------|-------|
| **Observer** | `RemoteAccessCommandObserver` inspects TCP command/response pairs and triggers the appropriate log method without modifying existing session handlers |
| **Layer** | Clean separation of concerns: Common library → TCP Server handlers → Centralized logging server |
| **Value Object** | `RemoteAccessLogEntry` and `ActiveSession` are immutable and compared by value |
| **Singleton-like Access** | `RemoteAccessLoggingConfig` provides static access to the effective configuration via cascading property resolution |
| **Active Record** | `RemoteAccessSessionTracker` encapsulates per-connection state and exposes predicate methods (`shouldLogDisconnect`) |
| **Thread-Safe Store** | `RemoteAccessLogStore` uses `synchronized` methods and returns unmodifiable copies to the HTTP handler |

## Module Dependency

```
aisafe.app.remote.logging.common (shared library)
├── RemoteAccessServiceId          enum
├── RemoteAccessLogEntry           value object
├── RemoteAccessDatagramParser     static parser
├── RemoteAccessUdpLogger          UDP emitter
├── RemoteAccessCommandObserver    static observer
├── RemoteAccessSessionTracker     per-connection state
└── RemoteAccessLoggingConfig      static config access
         ▲                    ▲                    ▲
         │                    │                    │
┌────────┴───────┐   ┌───────┴────────┐   ┌──────┴──────────┐
│ PilotLogging*  │   │ AtcLogging*    │   │ LoggingClient*  │
│ ClientHandler  │   │ ClientHandler  │   │ Handler         │
│ (pilot.server) │   │ (atc.server)   │   │ (weather.server)│
└────────────────┘   └────────────────┘   └─────────────────┘
         ▲                    ▲                    ▲
         │                    │                    │
┌────────┴────────┐  ┌───────┴────────┐  ┌───────┴─────────┐
│PilotServerWith* │  │ATCServerWith*  │  │WeatherServerWith│
│RemoteLogging    │  │RemoteLogging   │  │RemoteLogging    │
│ (port 8890)     │  │ (port 8888)    │  │ (port 8889)     │
└─────────────────┘  └────────────────┘  └─────────────────┘

aisafe.app.remote.logging.server (logging server node)
├── RemoteAccessLoggingServer   main entry point
├── RemoteAccessUdpReceiver     UDP receive loop
├── RemoteAccessLogStore        in-memory store + ActiveSession record
├── RemoteAccessHttpServer      HTTP listener thread
└── RemoteAccessHttpHandler     per-request handler
```

## Session Tracking State Machine

Each `RemoteAccessSessionTracker` instance manages the lifecycle of a single TCP connection:

```
                    ┌──────────────────────────────────────────┐
                    │                                          │
                    ▼                                          │
┌──────────┐   LOGIN/OK   ┌──────────────┐   LOGOUT        ┌──────────┐
│          │──────────────▶│              │────────────────▶│          │
│ ANONYMOUS│               │ AUTHENTICATED│                 │ LOGGED_OUT│
│          │               │              │                 │          │
└──────────┘               └──────────────┘                 └──────────┘
     │                            │
     │                            │ DISCONNECT (no prior LOGOUT)
     │                            ▼
     │                     ┌──────────────┐
     └────────────────────▶│  DISCONNECT  │
                           │   LOGGED     │
                           └──────────────┘
```

- `shouldLogDisconnect()` returns `true` only when `loggedInUsername != null && !disconnectLogged`.
- After logging the DISCONNECT, `markDisconnectLogged()` sets the flag, preventing duplicate events.

## UDP Protocol

**Format:** Each datagram carries a single line of pipe-delimited UTF-8 text.

```
eventType|timestamp|username|clientIp|clientPort|serviceId
```

**Field details:**

| Field | Constraints |
|-------|-------------|
| eventType | Must be one of LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, DISCONNECT |
| timestamp | Must be parseable as `java.time.Instant` (ISO-8601) |
| username | Any non-empty string; null/blank is replaced with `-` |
| clientIp | Transparently forwarded from the TCP socket |
| clientPort | Must be a valid integer |
| serviceId | Any string; services emit WEATHER, ATC, or PILOT |

**Validation** (`RemoteAccessDatagramParser`):
1. Payload must be non-blank.
2. Must have at least 6 pipe-separated fields.
3. Timestamp must be a valid ISO-8601 Instant.
4. Client port must be a valid integer.
5. Invalid datagrams are silently discarded (error printed to stderr).

## HTTP Dashboard Routes

| Route | Method | Content-Type | Description |
|-------|--------|--------------|-------------|
| `/` or `/index.html` | GET | text/html | Static landing page |
| `/events` | GET | text/html | Events browser (AJAX auto-refresh) |
| `/active-users` | GET | text/html | Active sessions browser (AJAX auto-refresh) |
| `/api/events` | GET | text/html | Dynamic HTML fragment: recent events table |
| `/api/active-users` | GET | text/html | Dynamic HTML fragment: active sessions table |

## Design Decisions

1. **UDP over TCP**: Fire-and-forget semantics prevent the TCP servers from being blocked by logging server unavailability. Packet loss is tolerated.
2. **Pipe-delimited text over binary**: Human-readable, debuggable, and trivially parsable. Reduces encoding risk across heterogeneous nodes.
3. **Static observer methods**: `RemoteAccessCommandObserver.observe(...)` and `.observeDisconnect(...)` accept all required collaborators as parameters, keeping integration lightweight and stateless.
4. **Per-connection tracker**: Each `RemoteAccessSessionTracker` is scoped to one TCP connection, eliminating shared mutable state and race conditions.
5. **Bounded in-memory store**: `RemoteAccessLogStore` caps recent events at 500 (circular `ArrayDeque`). Active sessions are derived from the event stream, not stored separately.
6. **Cascading configuration**: Environment variables → System properties → Classpath properties → Defaults. This allows per-deployment tuning without code changes.
