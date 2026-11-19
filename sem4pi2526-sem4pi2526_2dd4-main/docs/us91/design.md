# US91 — Design

## Architecture Overview

US91 adds the HTTP layer to the logging server node from US90. Both the UDP receiver thread and the HTTP server thread are started by `RemoteAccessLoggingServer.main()` and share a single `RemoteAccessLogStore` instance.

```
                         ┌──────────────────────────────────────────┐
                         │   RemoteAccessLoggingServer (main)       │
                         │                                          │
                         │  ┌───────────────────┐                  │
 UDP datagrams           │  │ RemoteAccessUdp   │                  │
 (from TCP servers) ────▶│  │ Receiver          │──▶ LogStore      │
                         │  │ (thread: udp-rx)  │         │        │
                         │  └───────────────────┘         │        │
                         │                                 │        │
                         │  ┌───────────────────┐         │        │
 Browser GET requests ──▶│  │ RemoteAccessHttp  │◀────────┘        │
                         │  │ Server            │                  │
                         │  │ (thread: http-srv)│                  │
                         │  │   spawns handler  │                  │
                         │  │   per connection  │                  │
                         │  └───────────────────┘                  │
                         └──────────────────────────────────────────┘
```

## Key Design Patterns

| Pattern | Usage |
|---------|-------|
| **Thread-per-connection** | `RemoteAccessHttpServer` spawns one daemon thread per browser connection, matching the existing pattern used by the Weather/ATC/Pilot TCP servers |
| **Router (switch dispatch)** | `RemoteAccessHttpHandler.route(path, out)` dispatches on the request path via a `switch` expression — simple, readable, no framework overhead |
| **Template (minimal HTTP)** | `sendBytes()` is the single point that formats the HTTP response header; all other methods call it, ensuring consistent formatting |
| **Read-Only View** | The handler queries `store.recentEvents()` and `store.activeSessions()` but never mutates the store — clear read/write boundary between US90 and US91 |
| **Classpath Resource Serving** | Static HTML pages are loaded via `getClass().getClassLoader().getResourceAsStream(...)`, bundled in the JAR, requiring no file-system access at runtime |

## Module Dependency

```
aisafe.app.remote.logging.common (shared library, US90)
└── RemoteAccessLoggingConfig      provides httpPort()

aisafe.app.remote.logging.server (this module)
├── RemoteAccessLoggingServer      main — creates LogStore, starts UDP receiver + HTTP server
├── RemoteAccessLogStore           in-memory store (US90) — read by HTTP handler
├── RemoteAccessHttpServer         TCP listener on HTTP port — spawns handler per request
└── RemoteAccessHttpHandler        parses HTTP request, routes, writes response

src/main/resources/www/
├── index.html                     landing page (navigation links)
├── events.html                    AJAX page for recent events
└── active-users.html              AJAX page for active sessions
```

## Thread Model

| Thread name | Class | Lifecycle |
|-------------|-------|-----------|
| `main` | `RemoteAccessLoggingServer` | Starts the two worker threads and registers shutdown hook |
| `remote-access-udp-receiver` | `RemoteAccessUdpReceiver` | Non-daemon; runs until interrupted |
| `remote-access-http-server` | `RemoteAccessHttpServer` | Non-daemon; blocks on `accept()`, interrupted on shutdown |
| `Thread-N` (one per request) | `RemoteAccessHttpHandler` | Daemon; terminates when the response is sent and the socket is closed |

## HTTP Routing Table

| Request | Action | Response |
|---------|--------|----------|
| `GET /` | Serve `www/index.html` from classpath | 200 + HTML |
| `GET /index.html` | Serve `www/index.html` from classpath | 200 + HTML |
| `GET /events` | Serve `www/events.html` from classpath | 200 + HTML |
| `GET /active-users` | Serve `www/active-users.html` from classpath | 200 + HTML |
| `GET /api/events` | Build events table from `store.recentEvents()` | 200 + HTML fragment |
| `GET /api/active-users` | Build sessions table from `store.activeSessions()` | 200 + HTML fragment |
| `GET <unknown>` | Not found | 404 + plain text |
| `POST/PUT/…` | Method not allowed | 405 + plain text |

## AJAX Page Design

Each AJAX page (`events.html`, `active-users.html`) uses the same self-rescheduling pattern:

```javascript
function refreshXxx() {
    var req = new XMLHttpRequest();
    req.timeout = 3000;
    req.onload = function () {
        if (this.status === 200) {
            document.getElementById("content").innerHTML = this.responseText;
        }
        setTimeout(refreshXxx, 1500);   // reschedule regardless of outcome
    };
    req.onerror = req.ontimeout = function () {
        document.getElementById("status").innerHTML = "Still trying...";
        setTimeout(refreshXxx, 1500);
    };
    req.open("GET", "/api/xxx");
    req.send();
}
```

Key properties:
- No `setInterval` — rescheduling happens *after* each response, so requests never pile up if the server is slow.
- 3000 ms request timeout prevents indefinite blocking.
- On any failure (network error, timeout, non-200), the page retries after 1500 ms.

## Configuration

| Property key | Environment variable | Default | Description |
|---|---|---|---|
| `remote.http.port` | — | `8080` | TCP port for the HTTP dashboard |

Resolution order (highest to lowest priority): environment variable `REMOTE_HTTP_PORT` → system property `remote.http.port` → `application.properties` on classpath → default `8080`.

The `httpPort()` method is added to `RemoteAccessLoggingConfig` (US90's config class) to keep all configuration in one place.

## Design Decisions

1. **Raw ServerSocket over embedded HTTP library**: Consistent with the RCOMP requirement to implement networking at a low level. Keeps the deployment a single JAR with no external dependencies.
2. **HTML fragments over JSON**: The AJAX endpoints return ready-to-inject HTML tables rather than JSON that would require client-side rendering. This avoids any JavaScript templating and keeps the client code minimal.
3. **Daemon threads for HTTP handler**: Browser connections are short-lived. Using daemon threads means they are automatically killed when the server shuts down, without explicit lifecycle management.
4. **Shared LogStore (read-only access)**: US91 reuses US90's `RemoteAccessLogStore` without modification. The HTTP handler only reads from it, ensuring no new synchronization concerns are introduced.
5. **HTML escaping in dynamic fragments**: All user-controlled fields (username, IP, service) are escaped before insertion into HTML to prevent XSS if a malicious client sends crafted data.
6. **`Connection: close` on every response**: The server does not implement HTTP keep-alive. Each browser connection is closed after serving one response, simplifying the handler lifecycle.
