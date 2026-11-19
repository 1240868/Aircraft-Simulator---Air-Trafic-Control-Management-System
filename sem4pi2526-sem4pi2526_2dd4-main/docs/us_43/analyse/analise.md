# US043 — Analysis

## 1. Overview

This document describes the domain analysis for **US043 — Consult Weather Data**.  
It identifies the relevant domain concepts, query model, repositories and layer responsibilities following **Domain-Driven Design (DDD)** tactical patterns.

> **Note:** The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_43/design/`.

---

## 2. Domain Concepts Identified

| Concept | Type | Justification |
|---------|------|---------------|
| `WeatherData` | **Aggregate Root (Entity)** | Pre-existing aggregate; read-only access in this US |
| `AirControlAreaCode` | **Value Object** | Query filter; normalised area identifier |
| `WeatherQueryDate` | **Value Object** | Wraps a calendar date; defines UTC day boundaries |
| `WeatherDataSummary` | **Read Model / DTO** | Presentation-friendly view of one observation |
| `WeatherConsultationResult` | **DTO** | List of summaries + query metadata |

---

## 3. Query Model

Consultation is a **read-only use case**. No aggregate mutation occurs.

### Query criteria

```
areaCode  : AirControlAreaCode   (required)
date      : WeatherQueryDate     (required, UTC calendar day)
```

### Result projection

Each matching `WeatherData` is mapped to:

```
WeatherDataSummary
├── weatherDataId
├── areaCode
├── observedAt
├── temperatureC
├── humidityPercent
├── windSpeedMs
├── windDirectionDegrees
└── source (if available)
```

Results sorted by `observedAt` ascending.

---

## 4. Aggregate Design (read-only)

### WeatherData Aggregate (reused)

No new invariants are introduced. The aggregate is retrieved but never modified.

```
WeatherData (Aggregate Root) — read-only in US043
├── WeatherDataId
├── AirControlAreaCode
├── observedAt
├── windSpeedMs
├── windDirectionDegrees
├── temperatureC
├── humidityPercent
└── WeatherSource (optional)
```

---

## 5. Value Objects Detail

### WeatherQueryDate
```
attributes:
  - date : LocalDate (valid calendar date)

methods:
  + startOfDayUtc() : Instant   → date at 00:00 UTC
  + endOfDayUtc()   : Instant   → date at 23:59:59.999 UTC
```
- Immutable
- Encapsulates UTC day range logic (BR1)

### AirControlAreaCode
- Reused from US041
- Normalised at construction: trim + uppercase

---

## 6. Repository Interfaces

### IWeatherDataRepository (query extension)
```java
public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {
    List<WeatherData> findByAreaAndDateRange(
        AirControlAreaCode areaCode,
        Instant fromInclusive,
        Instant toInclusive
    );
}
```

### IAirControlAreaRepository (pre-existing)
```java
public interface AirControlAreaRepository {
    Optional<AirControlArea> findByCode(AirControlAreaCode code);
}
```

---

## 7. Services

### 7.1 Application Service: `ConsultWeatherDataService`

Coordinates the read-only use case:

1. Authenticate and authorise the actor (WEATHER_PERSON, PILOT or FCO)
2. Normalise and validate `AirControlAreaCode`
3. Confirm area exists via `AirControlAreaRepository`
4. Build UTC day range from `WeatherQueryDate`
5. Query `WeatherDataRepository.findByAreaAndDateRange(...)`
6. Map results to `WeatherDataSummary` list
7. Return `WeatherConsultationResult`

No domain service is required — this is a query with lightweight validation.

---

## 8. DTOs

### WeatherConsultationRequest
```java
public record WeatherConsultationRequest(
    String areaCode,
    LocalDate date
) {}
```

### WeatherDataSummary
```java
public record WeatherDataSummary(
    String weatherDataId,
    String areaCode,
    Instant observedAt,
    double temperatureC,
    double humidityPercent,
    double windSpeedMs,
    double windDirectionDegrees,
    String source
) {}
```

### WeatherConsultationResult
```java
public record WeatherConsultationResult(
    String areaCode,
    LocalDate date,
    int recordCount,
    List<WeatherDataSummary> records
) {}
```

---

## 9. Layer Responsibilities Summary

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `ConsultWeatherDataUI` | Collect area + date, display results or empty message |
| **Application** | `ConsultWeatherDataController` | Authz, delegate to service |
| **Application** | `ConsultWeatherDataService` | Query coordination, area validation, DTO mapping |
| **Domain** | `WeatherData`, `AirControlAreaCode`, `WeatherQueryDate` | Value validation |
| **Infrastructure** | `JpaWeatherDataRepository` | Query implementation |

---

## 10. Key Design Decisions

| Decision | Justification |
|----------|---------------|
| Read-only use case | No aggregate mutation; simpler transaction model |
| UTC day boundaries in Value Object | Centralises date-range logic; avoids duplication |
| Area existence validated before query | Clear error for unknown areas vs empty result |
| Empty result is success | AC7 — no data is not a failure |
| Multiple roles allowed | Weather Person, Pilot and FCO all need access |
| Repository query by range | Efficient filter at persistence layer |

---

## 11. Functional Dependencies

| US | Relationship |
|----|-------------|
| US041 | Provides weather data |
| US042 | Provides bulk-imported weather data |
| US050 | Air control area prerequisite |
| US082 | Pilot selects consulted weather for flight plans |
| US030 | Authentication and authorisation |
