# US91 — HTTP Monitoring Dashboard

## Documents
| Document | Description |
|----------|-------------|
| [README](README.md) | US identification, acceptance criteria, dependencies, notes |
| [Requirements Engineering](requirements.md) | Actor analysis, use cases (UC91.1–UC91.3), non-functional requirements |
| [Analysis](analysis.md) | Domain model, attribute tables, HTTP protocol definition, glossary |
| [Design](design.md) | Architecture, design patterns, routing, HTML/AJAX design, decisions |

## Diagrams (PlantUML)

### Sequence Diagrams
| Diagram | File | Description |
|---------|------|-------------|
| Browse Recent Events | [SD_Browse_Events.puml](diagrams/SD_Browse_Events.puml) | Browser opens /events page and AJAX fetches /api/events — full request/response cycle |
| Browse Active Users | [SD_Browse_Active_Users.puml](diagrams/SD_Browse_Active_Users.puml) | Browser opens /active-users page and AJAX fetches /api/active-users |

### Class Diagram
| Diagram | File | Description |
|---------|------|-------------|
| Full CD | [CD_HttpDashboard.puml](diagrams/CD_HttpDashboard.puml) | Class diagram of the HTTP dashboard module: server, handler, and its relationship to the log store |
