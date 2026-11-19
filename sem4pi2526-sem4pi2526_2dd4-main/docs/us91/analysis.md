# US91 — Analysis

## Domain Model Overview

US91 adds an HTTP monitoring layer on top of the centralized logging server from US90. The domain is composed of two parts:

1. **HTTP Server** (`RemoteAccessHttpServer`) — a `Runnable` that listens for browser TCP connections and spawns a `RemoteAccessHttpHandler` per request.
2. **HTTP Handler** (`RemoteAccessHttpHandler`) — a `Runnable` that parses the HTTP request line, routes to the appropriate handler, and writes the HTTP response.

Both classes read from `RemoteAccessLogStore` (owned by US90) but never write to it. The store is the only shared mutable state and is already thread-safe (synchronized methods).

## Key Domain Concepts

| Concept | Description |
|---------|-------------|
| RemoteAccessHttpServer | TCP listener on the configured HTTP port; spawns one handler thread per browser connection |
| RemoteAccessHttpHandler | Parses a raw HTTP/1.1 GET request, routes by path, and writes the full HTTP response |
| RemoteAccessLogStore | Shared in-memory store (US90) that holds recent events and derives active sessions; read-only from HTTP handler |
| Static HTML pages | `index.html`, `events.html`, `active-users.html` — served as classpath resources from `src/main/resources/www/` |
| Dynamic HTML fragments | `/api/events` and `/api/active-users` — generated in-memory HTML tables injected via AJAX into the static pages |

## Attribute Tables

### RemoteAccessHttpServer
| Field | Type | Description |
|-------|------|-------------|
| port | int | TCP port on which the server listens for browser connections |
| store | RemoteAccessLogStore | Reference to the shared log store (read-only from HTTP perspective) |

### RemoteAccessHttpHandler
| Field | Type | Description |
|-------|------|-------------|
| client | Socket | The accepted browser TCP socket for this request |
| store | RemoteAccessLogStore | Reference to the shared log store, used to build dynamic fragments |

### HTTP Routes
| Path | Content | Type |
|------|---------|------|
| `/` or `/index.html` | `www/index.html` (classpath resource) | Static HTML |
| `/events` or `/events.html` | `www/events.html` (classpath resource) | Static HTML |
| `/active-users` or `/active-users.html` | `www/active-users.html` (classpath resource) | Static HTML |
| `/api/events` | Dynamic HTML table generated from `store.recentEvents()` | Dynamic HTML fragment |
| `/api/active-users` | Dynamic HTML table generated from `store.activeSessions()` | Dynamic HTML fragment |

## HTTP Protocol

The server implements a minimal subset of HTTP/1.1:

**Request parsing:**
- Reads only the first line (`GET /path HTTP/1.1`)
- Ignores request headers and body
- Only the `GET` method is accepted; others receive `405 Method Not Allowed`

**Response format:**
```
HTTP/1.1 <status> <statusText>\r\n
Content-Type: <contentType>\r\n
Content-Length: <length>\r\n
Connection: close\r\n
\r\n
<body bytes>
```

**Status codes used:**
| Code | Meaning | When |
|------|---------|------|
| 200 OK | Success | Static file found or dynamic fragment generated |
| 404 Not Found | Resource missing | Unknown path or missing classpath resource |
| 405 Method Not Allowed | Wrong method | Any non-GET request |

## AJAX Polling Protocol

The static HTML pages use XMLHttpRequest with a recursive `setTimeout` pattern:

```
onload:
  refreshXxx()
    → XHR GET /api/xxx
    → on success: innerHTML = responseText
    → setTimeout(refreshXxx, 1500)
    → on error/timeout: retry same
```

- Polling interval: **1500 ms** after each response (success or failure)
- Timeout per request: **3000 ms**
- On network error: page shows "Still trying... (connection error)" and retries

## HTML Output Format

**Events table columns** (most recent first):
`Time | Event | User | Client IP | Port | Service`

**Active users table columns**:
`User | Client IP | Port | Service | Connected since`

Timestamps are displayed in the server's local timezone using the format `yyyy-MM-dd HH:mm:ss`.

All string fields are HTML-escaped (`&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`) before insertion.

## Glossary

| Term | Definition |
|------|------------|
| HTTP Dashboard | The set of three HTML pages and two API endpoints served by `RemoteAccessHttpServer` |
| Dynamic Fragment | An HTML snippet returned by `/api/events` or `/api/active-users`, injected into the page via `innerHTML` without a full page reload |
| Classpath Resource | A file bundled in the JAR under `src/main/resources/` and accessed via `getClass().getClassLoader().getResourceAsStream(...)` |
| AJAX Polling | The pattern of periodically sending XMLHttpRequest calls to refresh content without user interaction |
| Active Session | A TCP connection where the user has authenticated (LOGIN_SUCCESS) and has not yet logged out or disconnected (derived by US90's log store) |
