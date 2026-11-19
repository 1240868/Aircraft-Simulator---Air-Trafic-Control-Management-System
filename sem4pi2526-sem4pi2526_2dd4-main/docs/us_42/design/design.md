# US042 — Design

## 1. Overview

This document describes the detailed software design for **US042 — Import Bulk Weather Data**, following a **Layered Architecture** with **DDD tactical patterns** and the **Strategy pattern** for extensible file parsing.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_42/design/`.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│              ImportWeatherDataUI                     │
└───────────────────────┬─────────────────────────────┘
                        │ uses DTO
┌───────────────────────▼─────────────────────────────┐
│                 APPLICATION LAYER                    │
│  ImportWeatherDataController                       │
│  ImportWeatherDataService                            │
│  WeatherImportResult / WeatherImportRowError         │
└──────────┬────────────────────────┬─────────────────┘
           │ uses domain objects    │ uses parser port
┌──────────▼────────────┐  ┌───────▼─────────────────┐
│     DOMAIN LAYER      │  │   PARSER PORT (Strategy) │
│  WeatherData          │  │  WeatherDataImportParser │
│  AirControlAreaCode   │  └───────┬─────────────────┘
│  WeatherSource        │          │ implemented by
└───────────────────────┘  ┌───────▼─────────────────┐
                           │  INFRASTRUCTURE LAYER    │
                           │  CsvWeatherDataImport    │
                           │    Parser                │
                           │  JpaWeatherDataRepository│
                           │  JpaAirControlAreaRepo   │
                           └─────────────────────────┘
```

---

## 3. Class Design

### 3.1 Domain Layer

#### `WeatherData` — Aggregate Root (reused from US041)

No structural changes. Optional `WeatherSource` field may be added to track the external provider.

```java
public class WeatherData implements AggregateRoot<WeatherDataId> {
    // existing fields from US041
    @Embedded
    private WeatherSource source; // optional extension for US042

    public WeatherData(AirControlAreaCode areaCode,
                       Instant observedAt,
                       double windSpeedMs,
                       double windDirectionDegrees,
                       double temperatureC,
                       double humidityPercent,
                       WeatherSource source) {
        // same validations as US041
        this.source = source != null ? source : WeatherSource.csvDefault();
    }
}
```

#### `WeatherSource` — Value Object

```java
public final class WeatherSource implements ValueObject {
    private final String name;

    public WeatherSource(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Source name is mandatory");
        }
        this.name = name.trim();
    }

    public static WeatherSource csvDefault() {
        return new WeatherSource("CSV-IMPORT");
    }
}
```

---

### 3.2 Parser Port — Strategy Pattern

```java
public interface WeatherDataImportParser {
    String supportedFormat(); // e.g. "CSV"
    List<WeatherImportRow> parse(InputStream input) throws ImportParseException;
}
```

#### `WeatherImportRow` — transient parsed row

```java
public record WeatherImportRow(
    int lineNumber,
    String areaCode,
    Instant observedAt,
    double temperatureC,
    double humidityPercent,
    double windSpeedMs,
    double windDirectionDegrees,
    String source
) {}
```

#### `CsvWeatherDataImportParser` — Infrastructure adapter

```java
public class CsvWeatherDataImportParser implements WeatherDataImportParser {

    private static final String FORMAT = "CSV";
    private static final char SEPARATOR = ';';

    @Override
    public String supportedFormat() { return FORMAT; }

    @Override
    public List<WeatherImportRow> parse(InputStream input) {
        // read lines, skip header if present, map columns, attach lineNumber
    }
}
```

---

### 3.3 Application Layer

#### `ImportWeatherDataController`

```java
@UseCaseController
public class ImportWeatherDataController {

    private final AuthorizationPort authz;
    private final ImportWeatherDataService service;

    public WeatherImportResult importFromFile(String format, Path filePath) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.WEATHER_PERSON);
        return service.importFromFile(format, filePath);
    }
}
```

#### `ImportWeatherDataService`

```java
public class ImportWeatherDataService {

    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;
    private final Map<String, WeatherDataImportParser> parsers;

    public WeatherImportResult importFromFile(String format, Path filePath) {
        WeatherDataImportParser parser = parsers.get(format.toUpperCase());
        if (parser == null) throw new UnsupportedImportFormatException(format);

        List<WeatherImportRow> rows = parser.parse(Files.newInputStream(filePath));
        List<WeatherImportRowError> errors = new ArrayList<>();
        int imported = 0;

        for (WeatherImportRow row : rows) {
            try {
                AirControlAreaCode code = AirControlAreaCode.valueOf(row.areaCode());
                if (areaRepo.findByCode(code).isEmpty()) {
                    throw new IllegalArgumentException("Unknown area: " + row.areaCode());
                }
                WeatherData wd = new WeatherData(
                    code, row.observedAt(),
                    row.windSpeedMs(), row.windDirectionDegrees(),
                    row.temperatureC(), row.humidityPercent(),
                    new WeatherSource(row.source())
                );
                weatherRepo.save(wd);
                imported++;
            } catch (Exception ex) {
                errors.add(new WeatherImportRowError(row.lineNumber(), ex.getMessage()));
            }
        }

        return new WeatherImportResult(rows.size(), imported, errors.size(), errors);
    }
}
```

#### Result DTOs

```java
public record WeatherImportResult(
    int totalRows, int importedCount, int rejectedCount,
    List<WeatherImportRowError> errors
) {}

public record WeatherImportRowError(int lineNumber, String message) {}
```

---

### 3.4 Presentation Layer

```java
public class ImportWeatherDataUI extends AbstractBaseUI {
    private final ImportWeatherDataController controller =
        new ImportWeatherDataController();

    protected boolean doShow() {
        String format = Console.readLine("Import format [CSV]: ");
        String path = Console.readLine("File path: ");
        WeatherImportResult result = controller.importFromFile(format, Path.of(path));
        // display summary and per-row errors
    }
}
```

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
UI → Controller: importFromFile(format, path)
Controller → Authz: ensureAuthenticatedUserHasAnyOf(WEATHER_PERSON)
Controller → Service: importFromFile(format, path)
  Service → Parser: parse(inputStream)
  loop each row:
    Service → AreaRepo: findByCode(code)
    Service → WeatherData: new WeatherData(...)
    Service → WeatherRepo: save(wd)   // on success
    Service → errors.add(...)         // on failure
  Service → Result: WeatherImportResult
Controller ← Service: WeatherImportResult
UI ← Controller: WeatherImportResult
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `WeatherDataImportParser` | Extensible support for new file formats |
| **Repository** | `WeatherDataRepository` | Abstracts persistence |
| **DTO** | `WeatherImportResult`, `WeatherImportRow` | Decouples layers |
| **Value Object** | `WeatherSource`, `AirControlAreaCode` | Self-validating, immutable |
| **Factory Method** | Parser registry in service | Selects parser by format name |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `UnsupportedImportFormatException` | Unknown format | Application |
| `ImportParseException` | Unreadable CSV structure | Infrastructure |
| `IllegalArgumentException` | Invalid VO / unknown area per row | Domain / Application |
| Row-level errors | Any per-row failure | Collected in `WeatherImportResult` |

---

## 7. Extensibility Notes

To add a new import source (e.g. JSON from an external API file):

1. Implement `WeatherDataImportParser` for the new format.
2. Register the parser in the service's parser map (via DI or factory).
3. No changes required in `ImportWeatherDataController` or `ImportWeatherDataUI`.

This satisfies the project requirement for easy expansion to new weather data sources.
