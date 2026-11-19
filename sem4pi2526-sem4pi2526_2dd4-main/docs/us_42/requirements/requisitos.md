# US042 — Import Bulk Weather Data

## 1. User Story

> **As a Weather Person, I want to import bulk weather data into the system.**

Weather data may originate from multiple external weather service providers. The system should aggregate data from various sources for better accuracy. While a simple **CSV file format** is available for initial system development, the architecture must be easy to expand to new weather data sources.

---

## 2. Context & Scope

This User Story extends the **Weather Service** bounded context. It complements **US041** (single-record registration) by allowing the Weather Person to load many observations at once from an external file.

The import process must:

- validate each record against the same domain rules as US041;
- report all invalid rows in a single execution;
- persist only valid records;
- remain open to new file formats and external providers without rewriting the use case.

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Weather Person** | Primary actor — selects and uploads a bulk data file |
| **System** | Parses the file, validates records, persists valid entries and reports results |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The Weather Person must be authenticated and authorised in the system |
| PC2 | The referenced **Air Control Areas** must exist in the system (US050) |
| PC3 | The import file must be accessible and readable by the application |
| PC4 | The file format must be supported by at least one registered import parser |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | All valid weather records from the file are persisted as `WeatherData` aggregates |
| PS2 | Invalid records are not persisted |
| PS3 | The system returns a summary: total rows, imported count, rejected count and per-row error details |
| PS4 | Each persisted record retains its **source/provider** identifier when supplied |

---

## 6. Main Flow (Basic Course of Events)

1. Weather Person selects the option to import bulk weather data.
2. System presents the list of supported import formats (initially CSV).
3. Weather Person selects a format and provides the file path.
4. System reads the file using the appropriate **import parser**.
5. For each parsed row, the system validates domain rules (area existence, numeric limits, date/time format).
6. Valid rows are converted into `WeatherData` aggregates and persisted.
7. Invalid rows are collected without stopping the entire import.
8. System presents an import summary to the Weather Person.

---

## 7. Alternative Flows

### AF1 — Unsupported file format
- At step 3, if the selected format is not supported, the system notifies the Weather Person and terminates the use case.

### AF2 — File not found or unreadable
- At step 4, if the file cannot be read, the system presents an error and terminates the use case.

### AF3 — Empty file
- At step 4, if the file contains no data rows, the system notifies the Weather Person and terminates without persisting anything.

### AF4 — All rows invalid
- At step 7, if every row fails validation, no records are persisted and the system presents the full error list.

### AF5 — Partial success
- At step 7, if some rows are valid and others invalid, valid rows are persisted and invalid rows are reported individually.

---

## 8. CSV Format (Initial Provider)

Header row (optional but recommended):

```csv
areaCode;observedAt;temperatureC;humidityPercent;windSpeedMs;windDirectionDegrees;source
```

Example data row:

```csv
PT-LIS;2026-05-29T14:00;18.5;62;5.2;270;METAR-Provider
```

| Column | Required | Description |
|--------|----------|-------------|
| `areaCode` | Yes | Air control area code (normalised: trim, uppercase) |
| `observedAt` | Yes | Observation timestamp (`yyyy-MM-ddTHH:mm`, UTC) |
| `temperatureC` | Yes | Temperature in degrees Celsius |
| `humidityPercent` | Yes | Relative humidity in [0, 100] |
| `windSpeedMs` | Yes | Wind speed in m/s (≥ 0) |
| `windDirectionDegrees` | Yes | Wind direction in [0, 360[ degrees |
| `source` | No | External provider name (defaults to `CSV-IMPORT`) |

Separator: semicolon (`;`). Decimal separator: dot (`.`).

---

## 9. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Only authenticated users with role **WEATHER_PERSON** may import bulk data |
| AC2 | The system supports at least one bulk import format (CSV) |
| AC3 | Each imported record must reference an existing air control area |
| AC4 | Domain validation rules match US041 (wind, humidity, direction, timestamps) |
| AC5 | Invalid rows do not prevent valid rows from being imported in the same execution |
| AC6 | The import result reports total, success and failure counts |
| AC7 | Per-row errors include line number and a meaningful message |
| AC8 | The import mechanism is extensible to new parsers/providers without changing the controller contract |
| AC9 | Imported records are persisted through `WeatherDataRepository` |
| AC10 | Source/provider information is stored when available |

---

## 10. Business Rules

| ID | Rule |
|----|------|
| BR1 | Each row produces at most one `WeatherData` aggregate |
| BR2 | Unknown air control area codes reject the corresponding row |
| BR3 | Duplicate rows (same area + observedAt) may be rejected or flagged according to project policy; the decision must be documented in implementation |
| BR4 | Physical limits are enforced at Value Object / aggregate construction time |
| BR5 | Import parsers must not access the database directly; persistence goes through repositories |

---

## 11. Related User Stories

| US | Relationship |
|----|-------------|
| US041 | Register weather data — single-record equivalent; shared domain rules |
| US043 | Consult weather data — consumes imported data |
| US050 | Register air control area — prerequisite |
| US030 | Authentication and authorisation — cross-cutting prerequisite |
| US044 | Weather Person remote TCP access (RCOMP) — expõe US041/042/043 |

---

## 12. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% for domain and controller packages |
| NFR08 | Persistence must be configurable (in-memory / relational RDBMS) |
| NFR09 | Authentication and authorisation must be enforced |
| NFR10 | Implementation language: Java |

---

## 13. Out of Scope for this US

- Real-time integration with external weather APIs
- Automatic scheduled imports
- Weather data consultation (US043)
- Flight plan weather association (US082)
- Simulation environmental threads (US110)
