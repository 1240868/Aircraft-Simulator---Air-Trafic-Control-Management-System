# US91 — Requirements Engineering

## Actor Identification

| Actor | Description |
|-------|-------------|
| Service Administrator | Human actor who opens a web browser to view access events and active sessions on the HTTP dashboard. |
| (System) Remote Access Logging Server | System actor that owns and serves the HTTP dashboard. Integrates with US90's log store. |

## Use Cases

### UC91.1 — View Recent Events via Browser
**Primary Actor:** Service Administrator
**Pre-condition:** The logging server (US90) is running with the HTTP dashboard started
**Post-condition:** Administrator sees a table of recent access events in the browser
**Main Success Scenario:**
1. Administrator opens `http://<logging-server>:8080/events` in a web browser.
2. The browser receives the static `events.html` page.
3. On page load, `refreshEvents()` executes and sends a GET request to `/api/events`.
4. The server responds with a dynamic HTML table of the most recent events (up to 500), most recent first.
5. The table displays: timestamp, event type, user, client IP, client port, and service.
6. After each successful response, `setTimeout(refreshEvents, 1500)` schedules the next poll.
7. The table continuously refreshes every ~1500 ms.

**Alternative — No Events Yet:**
- Step 4: The server returns `<p>No events recorded yet.</p>`.
- The page shows the placeholder message and continues polling.

### UC91.2 — View Active Sessions via Browser
**Primary Actor:** Service Administrator
**Pre-condition:** The logging server (US90) is running with the HTTP dashboard started
**Post-condition:** Administrator sees a table of currently logged-in sessions
**Main Success Scenario:**
1. Administrator opens `http://<logging-server>:8080/active-users` in a web browser.
2. The browser receives the static `active-users.html` page.
3. On page load, `refreshUsers()` executes and sends a GET request to `/api/active-users`.
4. The server responds with a dynamic HTML table of active sessions.
5. The table displays: user, client IP, client port, service, and connected-since timestamp.
6. The table auto-refreshes every ~1500 ms via `setTimeout`.

**Alternative — No Active Sessions:**
- Step 4: The server returns `<p>No users currently connected.</p>`.

### UC91.3 — Navigate to Dashboard Landing Page
**Primary Actor:** Service Administrator
**Pre-condition:** The logging server is running
**Post-condition:** Administrator sees the dashboard index page with navigation links
**Main Success Scenario:**
1. Administrator opens `http://<logging-server>:8080/` in a web browser.
2. The server returns the static `index.html` landing page.
3. The page provides navigation links to `/events` and `/active-users`.

## Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR1 | The HTTP server must be implemented using raw `java.net.ServerSocket` — no HTTP library or framework |
| NFR2 | Each browser connection must be handled in a separate daemon thread to avoid blocking |
| NFR3 | The HTTP port must be configurable via `remote.http.port` property (environment variable → system property → classpath property → default 8080) |
| NFR4 | No third-party JavaScript libraries — only plain XMLHttpRequest |
| NFR5 | Dynamic HTML fragments must escape HTML special characters (`&`, `<`, `>`) to prevent XSS in displayed data |
| NFR6 | The HTTP server must not interfere with the UDP receiver thread — both must run concurrently as independent threads |
