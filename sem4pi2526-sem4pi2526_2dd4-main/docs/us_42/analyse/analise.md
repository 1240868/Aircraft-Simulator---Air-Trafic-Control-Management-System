# US042 — Analysis

## 1. Overview

This document describes the domain analysis for **US042 — Import Bulk Weather Data**.  
It identifies the relevant domain concepts, aggregates, value objects, repositories, services and design patterns following **Domain-Driven Design (DDD)** tactical patterns.

> **Note:** The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_42/design/`.

---

## 2. Domain Concepts Identified

| Concept | Type | Justification |
|---------|------|---------------|
| `WeatherData` | **Aggregate Root (Entity)** | Pre-existing aggregate from US041; each imported row becomes one instance |
| `AirControlAreaCode` | **Value Object** | Normalised area identifier; shared with US041/US043 |
| `WeatherSource` | **Value Object** | Identifies the external provider (CSV, METAR, etc.) |
| `WeatherImportRow` | **Value Object / DTO** | Transient representation of one parsed file row before domain conversion |
| `WeatherImportResult` | **Value Object** | Summary of import execution (counts + row errors) |
| `WeatherImportRowError` | **Value Object** | Line number + error message for a rejected row |
| `WeatherDataImportParser` | **Domain/Application Port** | Strategy interface for format-specific parsing |
| `CsvWeatherDataImportParser` | **Infrastructure Adapter** | Initial CSV implementation |

---

## 3. Aggregate Design

### 3.1 WeatherData Aggregate (reused from US041)

Each valid imported row produces one `WeatherData` aggregate with the same invariants as manual registration:

```
WeatherData (Aggregate Root)
├── WeatherDataId          (identity)
├── AirControlAreaCode     (Value Object)
├── observedAt             (Instant)
├── windSpeedMs            (double, ≥ 0)
├── windDirectionDegrees   (double, [0, 360[)
├── temperatureC           (double)
├── humidityPercent        (double, [0, 100])
└── WeatherSource          (Value Object, optional)
```

**Invariants enforced at construction:**

| Invariant | Description |
|-----------|-------------|
| INV1 | `AirControlAreaCode` must not be null |
| INV2 | `observedAt` must not be null |
| INV3 | Wind speed ≥ 0 |
| INV4 | Wind direction in [0, 360[ degrees |
| INV5 | Humidity in [0, 100] percent |

Cross-aggregate rule (area existence) is validated at **Application Service** level before aggregate creation.

---

## 4. Import Architecture — Strategy Pattern

Bulk import is decoupled from file format through a parser port:

```
WeatherDataImportParser (interface)
    ├── parse(InputStream) → List<WeatherImportRow>
    └── supportedFormat() → String

CsvWeatherDataImportParser (implementation)
FutureJsonWeatherDataImportParser (extension point)
```

This satisfies the project requirement that the system must be **easy to expand to new weather data sources**.

---

## 5. Repository Interfaces

### IWeatherDataRepository (extended usage)
```java
public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {
    WeatherData save(WeatherData data);
    // existing methods from US041/US043
}
```

### IAirControlAreaRepository (pre-existing)
```java
public interface AirControlAreaRepository {
    Optional<AirControlArea> findByCode(AirControlAreaCode code);
}
```

---

## 6. Services

### 6.1 Application Service: `ImportWeatherDataService`

Coordinates the bulk import use case:

1. Authenticate and authorise the Weather Person
2. Select the appropriate parser for the requested format
3. Parse the file into transient `WeatherImportRow` objects
4. For each row:
   - validate area existence via `AirControlAreaRepository`
   - construct `WeatherData` (domain validation)
   - persist via `WeatherDataRepository` on success
   - collect `WeatherImportRowError` on failure
5. Build and return `WeatherImportResult`

### 6.2 Parser: `CsvWeatherDataImportParser`

Responsibilities:

- Read semicolon-separated CSV
- Skip or validate header row
- Map columns to `WeatherImportRow`
- Report syntax/parse errors with line numbers

---

## 7. DTOs

### WeatherImportResult
```java
public record WeatherImportResult(
    int totalRows,
    int importedCount,
    int rejectedCount,
    List<WeatherImportRowError> errors
) {}
```

### WeatherImportRowError
```java
public record WeatherImportRowError(
    int lineNumber,
    String message
) {}
```

---

## 8. Layer Responsibilities Summary

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `ImportWeatherDataUI` | File path/format selection, display import summary |
| **Application** | `ImportWeatherDataController` | Orchestrates use case, enforces authz |
| **Application** | `ImportWeatherDataService` | Row-by-row validation and persistence coordination |
| **Domain** | `WeatherData`, `AirControlAreaCode`, `WeatherSource` | Business invariants |
| **Infrastructure** | `CsvWeatherDataImportParser` | CSV parsing adapter |
| **Infrastructure** | `JpaWeatherDataRepository` | Persistence |

---

## 9. Key Design Decisions

| Decision | Justification |
|----------|---------------|
| Strategy pattern for parsers | Enables new formats without changing the use case |
| Partial import allowed | Invalid rows must not block valid ones (AC5) |
| Area existence checked in Application Service | Cross-aggregate validation; keeps WeatherData focused |
| `WeatherImportRow` is not an aggregate | Transient parsed data; only WeatherData is persisted |
| Source stored as Value Object | Supports multi-provider aggregation requirement |
| All errors collected in one execution | Aligns with DSL semantic validation pattern in project |

---

## 10. Functional Dependencies

| US | Relationship |
|----|-------------|
| US041 | Shared WeatherData aggregate and validation rules |
| US050 | Air control areas must exist |
| US043 | Imported data becomes consultable |
| US030 | Authentication and authorisation |
