# US91 - HTTP Monitoring Dashboard

## Identification
- **US Number:** US91
- **Title:** HTTP Monitoring Dashboard for Remote Access Events
- **Actor:** Service Administrator (human)
- **Component:** RCOMP (Remote Communication)
- **Module:** `rcomp.base`

## Description
As a Service Administrator, I want to be able to consult the access logs of the system's remote accesses from a web browser so that I can monitor the operation of the system without needing dedicated client software.

The solution extends the Remote Access Logging Server (US90) with a built-in HTTP server that serves an interactive monitoring dashboard. The dashboard provides two auto-refreshing pages: one for recent access events and one for currently active sessions. Data is served as dynamic HTML fragments via AJAX, so the pages update in real-time without full page reloads.

## Acceptance Criteria
1. The logging server must expose an HTTP dashboard on a configurable port (default 8080).
2. The dashboard must provide a page listing recent access events with timestamp, event type, user, client IP, client port, and service.
3. The dashboard must provide a page listing currently active (logged-in) sessions with user, client IP, client port, service, and connection timestamp.
4. Both pages must auto-refresh via AJAX (XMLHttpRequest) without full page reloads, with a polling interval of 1500 ms.
5. The HTTP port must be configurable via `remote.http.port` in `application.properties`, system properties, or environment variables.
6. No client-side frameworks or external libraries may be used — plain HTML and XMLHttpRequest only.

## Dependencies
- **US90** — Remote Access Logging (provides `RemoteAccessLogStore`, `RemoteAccessLoggingServer`, and `RemoteAccessLoggingConfig`)

## Notes
- The HTTP server is implemented from scratch using `java.net.ServerSocket` — no embedded HTTP server library (e.g. Jetty, Undertow) is used, in keeping with the RCOMP low-level networking requirement.
- Each browser request is handled in a dedicated daemon thread spawned by `RemoteAccessHttpServer`.
- The dashboard is non-secure (plain HTTP) and is intended for use on a trusted monitoring network only.
- The `/api/events` and `/api/active-users` endpoints return raw HTML table fragments (not JSON), so they can be injected directly into the page's `innerHTML`.
