# US90 — Remote Access Logging

## Documents
| Document | Description |
|----------|-------------|
| [README](README.md) | US identification, acceptance criteria, dependencies, notes |
| [Requirements Engineering](requirements.md) | Actor analysis, use cases (UC90.1–UC90.6), non-functional requirements |
| [Analysis](analysis.md) | Domain model, attribute tables, protocol definition, glossary |
| [Design](design.md) | Architecture, design patterns, state machine, protocol details, decisions |

## Diagrams (PlantUML)

### Sequence Diagrams
| Diagram | File | Description |
|---------|------|-------------|
| Login Success | [SD_Login_Success.puml](diagrams/SD_Login_Success.puml) | Successful LOGIN command flow through observer, logger, UDP receiver, and store |
| Login Failure | [SD_Login_Failure.puml](diagrams/SD_Login_Failure.puml) | Failed LOGIN command — observer detects ERROR response and emits LOGIN_FAILURE |
| Logout | [SD_Logout.puml](diagrams/SD_Logout.puml) | LOGOUT command — observer emits LOGOUT via logger, tracker transitions to logged-out |
| Disconnect | [SD_Disconnect.puml](diagrams/SD_Disconnect.puml) | Unexpected TCP disconnect — observer emits DISCONNECT via logger |

### Class Diagram
| Diagram | File | Description |
|---------|------|-------------|
| Full CD | [CD_RemoteAccessLogging.puml](diagrams/CD_RemoteAccessLogging.puml) | Complete class diagram of all modules, packages, relationships, and method signatures |
