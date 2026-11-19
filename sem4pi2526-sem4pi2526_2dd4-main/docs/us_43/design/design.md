# US043 — Design

> **Implementação actual:** repositório `findByAreaCodeBetween(..., fromInclusive, toExclusive)`; `WeatherQueryDate.endOfDayExclusiveUtc()`; DTOs em `weather.application.consultation`; `WeatherSource.value()` (não `name()`).

## 1. Overview

This document describes the detailed software design for **US043 — Consult Weather Data**, following a **Layered Architecture** with **DDD tactical patterns**.

This is a **read-only query use case** — no aggregate mutation occurs.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_43/design/`.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│              ConsultWeatherDataUI                    │
└───────────────────────┬─────────────────────────────┘
                        │ uses DTO
┌───────────────────────▼─────────────────────────────┐
│                 APPLICATION LAYER                    │
│  ConsultWeatherDataController                      │
│  ConsultWeatherDataService                           │
│  WeatherConsultationResult / WeatherDataSummary      │
└──────────┬────────────────────────┬─────────────────┘
           │ query                  │ query
┌──────────▼────────────┐  ┌───────▼─────────────────┐
│     DOMAIN LAYER      │  │   REPOSITORY INTERFACES  │
│  WeatherData          │  │  WeatherDataRepository   │
│  AirControlAreaCode   │  │  AirControlAreaRepository│
│  WeatherQueryDate     │  └───────┬─────────────────┘
└───────────────────────┘          │ implemented by
                           ┌───────▼─────────────────┐
                           │  INFRASTRUCTURE LAYER    │
                           │  JpaWeatherDataRepository│
                           │  JpaAirControlAreaRepo   │
                           └─────────────────────────┘
```

---

## 3. Class Design

### 3.1 Domain Layer

#### `WeatherQueryDate` — Value Object

```java
public final class WeatherQueryDate implements ValueObject {

    private final LocalDate date;

    public WeatherQueryDate(LocalDate date) {
        Objects.requireNonNull(date, "Date is mandatory");
        this.date = date;
    }

    public Instant startOfDayUtc() {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Instant endOfDayExclusiveUtc() {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public LocalDate value() { return date; }
}
```

#### `WeatherData` — Aggregate Root (read-only)

Existing aggregate from US041/US042. No modifications in this US.

---

### 3.2 Repository Interface — query extension

```java
public interface WeatherDataRepository
        extends DomainRepository<WeatherDataId, WeatherData> {

    Iterable<WeatherData> findByAreaCodeBetween(
        AirControlAreaCode areaCode,
        Instant fromInclusive,
        Instant toExclusive
    );
}
```

JPA implementation example:

```java
public class JpaWeatherDataRepository implements WeatherDataRepository {

    @Override
    public List<WeatherData> findByAreaAndDateRange(
            AirControlAreaCode areaCode,
            Instant fromInclusive,
            Instant toInclusive) {
        // JPQL: WHERE areaCode = :code
        //   AND observedAt >= :from AND observedAt <= :to
        // ORDER BY observedAt ASC
    }
}
```

---

### 3.3 Application Layer

#### `ConsultWeatherDataController`

```java
@UseCaseController
public class ConsultWeatherDataController {

    private final AuthorizationPort authz;
    private final ConsultWeatherDataService service;

    public WeatherConsultationResult consult(String areaCode, LocalDate date) {
        authz.ensureAuthenticatedUserHasAnyOf(
            Roles.WEATHER_PERSON,
            Roles.PILOT,
            Roles.FLIGHT_CONTROL_OPERATOR
        );
        return service.consult(areaCode, date);
    }
}
```

#### `ConsultWeatherDataService`

```java
public class ConsultWeatherDataService {

    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;

    public WeatherConsultationResult consult(String areaCodeRaw, LocalDate date) {
        AirControlAreaCode code = AirControlAreaCode.valueOf(areaCodeRaw);
        if (areaRepo.findByCode(code).isEmpty()) {
            throw new IllegalArgumentException("Unknown air control area: " + areaCodeRaw);
        }

        WeatherQueryDate queryDate = new WeatherQueryDate(date);
        List<WeatherData> records = weatherRepo.findByAreaAndDateRange(
            code,
            queryDate.startOfDayUtc(),
            queryDate.endOfDayUtc()
        );

        List<WeatherDataSummary> summaries = records.stream()
            .map(this::toSummary)
            .toList();

        return new WeatherConsultationResult(
            code.value(), date, summaries.size(), summaries
        );
    }

    private WeatherDataSummary toSummary(WeatherData wd) {
        return new WeatherDataSummary(
            wd.identity().toString(),
            wd.areaCode().value(),
            wd.observedAt(),
            wd.temperatureC(),
            wd.humidityPercent(),
            wd.windSpeedMs(),
            wd.windDirectionDegrees(),
            wd.source() != null ? wd.source().name() : "N/A"
        );
    }
}
```

#### DTOs

```java
public record WeatherConsultationResult(
    String areaCode,
    LocalDate date,
    int recordCount,
    List<WeatherDataSummary> records
) {}

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

---

### 3.4 Presentation Layer

```java
public class ConsultWeatherDataUI extends AbstractBaseUI {

    private final ConsultWeatherDataController controller =
        new ConsultWeatherDataController();

    protected boolean doShow() {
        String areaCode = Console.readLine("Air control area code: ");
        LocalDate date = LocalDate.parse(
            Console.readLine("Date (yyyy-MM-dd): ").trim()
        );

        WeatherConsultationResult result = controller.consult(areaCode, date);

        if (result.recordCount() == 0) {
            System.out.println("No weather data found for "
                + result.areaCode() + " on " + result.date() + ".");
        } else {
            // display table of summaries ordered by observedAt
        }
    }
}
```

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
UI → Controller: consult(areaCode, date)
Controller → Authz: ensureAuthenticatedUserHasAnyOf(...)
Controller → Service: consult(areaCode, date)
  Service → AirControlAreaCode: valueOf(areaCode)
  Service → AreaRepo: findByCode(code)
  Service → WeatherQueryDate: new WeatherQueryDate(date)
  Service → WeatherRepo: findByAreaAndDateRange(code, from, to)
  Service → Mapper: toSummary(each record)
  Service → Result: WeatherConsultationResult
Controller ← Service: WeatherConsultationResult
UI ← Controller: WeatherConsultationResult
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository** | `WeatherDataRepository` | Abstracts query persistence |
| **DTO** | `WeatherConsultationResult`, `WeatherDataSummary` | Read model for presentation |
| **Value Object** | `WeatherQueryDate`, `AirControlAreaCode` | Encapsulates date-range logic |
| **Query Object** | Implicit via method parameters | Clean read-only use case |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `IllegalArgumentException` | Unknown area code | Application |
| `DateTimeParseException` | Invalid date format | Presentation |
| Empty result | No records for area+date | Not an error — empty list returned |

---

## 7. Final Design Considerations

The design prioritises:

- read-only semantics (no side effects);
- clear separation between "unknown area" (error) and "no data" (empty result);
- reusable query method for US082 (Pilot selects weather by area and date);
- UTC day boundaries encapsulated in `WeatherQueryDate`.
