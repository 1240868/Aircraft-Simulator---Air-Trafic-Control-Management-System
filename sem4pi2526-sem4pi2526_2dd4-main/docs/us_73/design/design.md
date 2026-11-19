# US073 — Design

> **Implementação actual:** package `eapli.aisafe.flightroute` (não `flightPlan`); repositórios `FlightRouteRepository`, `AirportRepository`, `AirTransportCompanyRepository`; ver [`readme.md`](../readme.md) para mapeamento exacto ao código.

## 1. Overview

This document describes the detailed software design for **US073 — Create a Flight Route**, following a **Layered Architecture** with **DDD tactical patterns**.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_73/design/`.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                  │
│              CreateFlightRouteUI                     │
└───────────────────────┬─────────────────────────────┘
                        │ uses DTO
┌───────────────────────▼─────────────────────────────┐
│                 APPLICATION LAYER                    │
│  CreateFlightRouteController                         │
│  CreateFlightRouteService                            │
│  FlightRouteDTO / CreateFlightRouteRequest           │
└──────────┬────────────────────────┬─────────────────┘
           │ uses domain objects    │ calls
┌──────────▼────────────┐  ┌───────▼─────────────────┐
│     DOMAIN LAYER      │  │   REPOSITORY INTERFACES  │
│  FlightRoute          │  │  IFlightRouteRepository  │
│  FlightRouteName      │  │  IAirportRepository      │
│  Distance, Duration   │  │  ICompanyRepository      │
│  RouteDistanceCalc    │  └───────┬─────────────────┘
└───────────────────────┘          │ implemented by
                           ┌───────▼─────────────────┐
                           │  INFRASTRUCTURE LAYER    │
                           │  FlightRouteJpaRepository│
                           │  AirportJpaRepository    │
                           │  CompanyJpaRepository    │
                           └─────────────────────────┘
```

---

## 3. Class Design

### 3.1 Domain Layer

#### `FlightRoute` — Aggregate Root

```java
public class FlightRoute implements AggregateRoot<FlightRouteId> {

    private FlightRouteId id;
    private FlightRouteName name;
    private CompanyId companyId;
    private AirportId originAirportId;
    private AirportId destinationAirportId;
    private RouteActivationStatus status;
    private Distance distance;
    private Duration duration;

    public static FlightRoute create(
        FlightRouteName name,
        CompanyId companyId,
        AirportId originAirportId,
        AirportId destinationAirportId,
        Distance distance,
        Duration duration
    ) {
        if (originAirportId.equals(destinationAirportId)) {
            throw new DomainException("Origin must differ from destination");
        }
        return new FlightRoute(name, companyId, originAirportId,
                               destinationAirportId, distance, duration);
    }

    private FlightRoute(...) {
        this.id = FlightRouteId.newId();
        this.status = RouteActivationStatus.active();
        // assign fields, validate not null
    }

    public boolean isActive() { return status.isActive(); }
}
```

#### `FlightRouteName` — Value Object

```java
public final class FlightRouteName implements ValueObject {

    private static final Pattern FORMAT =
        Pattern.compile("^[A-Z]{2}\\d{1,4}$");

    private final String value;

    public FlightRouteName(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid route name: " + value
                + ". Expected: 2 letters + 1-4 digits (e.g. TP123)");
        }
        this.value = value;
    }

    public static FlightRouteName compose(String iataPrefix, String numericSuffix) {
        return new FlightRouteName(iataPrefix.toUpperCase() + numericSuffix);
    }

    public String value() { return value; }
}
```

#### `Distance` and `Duration` — Value Objects

```java
public final class Distance implements ValueObject {
    private final double valueKm;
    public Distance(double valueKm) {
        if (valueKm <= 0) throw new IllegalArgumentException("Distance must be positive");
        this.valueKm = valueKm;
    }
}

public final class Duration implements ValueObject {
    private final double minutes;
    public Duration(double minutes) {
        if (minutes <= 0) throw new IllegalArgumentException("Duration must be positive");
        this.minutes = minutes;
    }
}
```

#### `RouteActivationStatus` — Value Object

```java
public final class RouteActivationStatus implements ValueObject {
    private final boolean active;
    private final LocalDate activeUntil;

    public static RouteActivationStatus active() {
        return new RouteActivationStatus(true, null);
    }
    public boolean isActive() { return active; }
}
```

---

### 3.2 Domain Service

```java
public interface RouteDistanceCalculator {
    Distance calculateDistance(Coordinates origin, Coordinates destination);
    Duration estimateDuration(Distance distance, double cruiseSpeedKmh);
}

public class HaversineRouteDistanceCalculator implements RouteDistanceCalculator {

    @Override
    public Distance calculateDistance(Coordinates origin, Coordinates destination) {
        // Haversine formula on lat/lon
    }

    @Override
    public Duration estimateDuration(Distance distance, double cruiseSpeedKmh) {
        double hours = distance.valueKm() / cruiseSpeedKmh;
        return new Duration(hours * 60);
    }
}
```

---

### 3.3 Repository Interfaces

```java
public interface IFlightRouteRepository
        extends DomainRepository<FlightRouteId, FlightRoute> {
    Optional<FlightRoute> findByName(FlightRouteName name);
    boolean existsByName(FlightRouteName name);
    FlightRoute save(FlightRoute route);
}

public interface IAirportRepository {
    Optional<Airport> findById(AirportId id);
}

public interface IAirTransportCompanyRepository {
    Optional<AirTransportCompany> findById(CompanyId id);
    Optional<CompanyId> findCompanyIdForCollaboratorUsername(String username);
}
```

---

### 3.4 Application Layer

#### `CreateFlightRouteController`

```java
@UseCaseController
public class CreateFlightRouteController {

    private final AuthorizationPort authz;
    private final CreateFlightRouteService service;

    public FlightRouteDTO createRoute(CreateFlightRouteRequest request) {
        authz.ensureAuthenticatedUserHasAnyOf(
            Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        return service.createRoute(request, authz.currentUsername());
    }
}
```

#### `CreateFlightRouteService`

```java
public class CreateFlightRouteService {

    private final IFlightRouteRepository routeRepo;
    private final IAirportRepository airportRepo;
    private final IAirTransportCompanyRepository companyRepo;
    private final RouteDistanceCalculator distanceCalculator;

    public FlightRouteDTO createRoute(
            CreateFlightRouteRequest request,
            String username) {

        // 1. Resolve company
        CompanyId companyId = companyRepo
            .findCompanyIdForCollaboratorUsername(username)
            .orElseThrow(() -> new IllegalStateException("User not linked to company"));
        AirTransportCompany company = companyRepo.findById(companyId)
            .orElseThrow();

        // 2. Build and validate route name
        FlightRouteName name = FlightRouteName.compose(
            company.iataCode().value(), request.numericSuffix());
        if (routeRepo.existsByName(name)) {
            throw new DuplicateRouteNameException(name.value());
        }

        // 3. Load and validate airports
        Airport origin = airportRepo.findById(request.originAirportId())
            .orElseThrow(() -> new AirportNotFoundException(request.originAirportId()));
        Airport destination = airportRepo.findById(request.destinationAirportId())
            .orElseThrow(() -> new AirportNotFoundException(request.destinationAirportId()));
        if (!origin.isOperational() || !destination.isOperational()) {
            throw new InactiveAirportException();
        }

        // 4. Calculate distance and duration
        Distance distance = distanceCalculator.calculateDistance(
            origin.coordinates(), destination.coordinates());
        Duration duration = distanceCalculator.estimateDuration(distance, 850.0);

        // 5. Create and persist
        FlightRoute route = FlightRoute.create(
            name, companyId,
            origin.id(), destination.id(),
            distance, duration
        );
        FlightRoute saved = routeRepo.save(route);

        return FlightRouteMapper.toDTO(saved, origin, destination, company);
    }
}
```

#### Request / Response DTOs

```java
public record CreateFlightRouteRequest(
    String numericSuffix,      // 1-4 digits, e.g. "123"
    AirportId originAirportId,
    AirportId destinationAirportId
) {}

public record FlightRouteDTO(
    String routeName,
    String companyName,
    String originAirportIata,
    String destinationAirportIata,
    double distanceKm,
    double durationMinutes,
    String status
) {}
```

---

### 3.5 Presentation Layer

```java
public class CreateFlightRouteUI extends AbstractBaseUI {

    private final CreateFlightRouteController controller =
        new CreateFlightRouteController();

    protected boolean doShow() {
        // list operational airports
        String suffix = Console.readLine("Route number (1-4 digits): ");
        // select origin and destination
        FlightRouteDTO result = controller.createRoute(
            new CreateFlightRouteRequest(suffix, originId, destId));
        System.out.println("Route created: " + result.routeName());
    }
}
```

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
UI → Controller: createRoute(request)
Controller → Authz: ensureAuthenticatedUserHasAnyOf(ATCC)
Controller → Service: createRoute(request, username)
  Service → CompanyRepo: findCompanyIdForCollaboratorUsername(username)
  Service → CompanyRepo: findById(companyId)
  Service → FlightRouteName: compose(iata, suffix)
  Service → RouteRepo: existsByName(name)
  Service → AirportRepo: findById(originId)
  Service → AirportRepo: findById(destId)
  Service → Airport: isOperational() × 2
  Service → DistanceCalculator: calculateDistance(origin.coords, dest.coords)
  Service → DistanceCalculator: estimateDuration(distance, cruiseSpeed)
  Service → FlightRoute: create(name, companyId, originId, destId, distance, duration)
  Service → RouteRepo: save(route)
  Service → Mapper: toDTO(saved, origin, dest, company)
Controller ← Service: FlightRouteDTO
UI ← Controller: FlightRouteDTO
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Factory Method** | `FlightRoute.create(...)` | Controlled creation with invariant checks |
| **Repository** | `IFlightRouteRepository` | Abstracts persistence |
| **DTO** | `CreateFlightRouteRequest`, `FlightRouteDTO` | Decouples domain from presentation |
| **Value Object** | `FlightRouteName`, `Distance`, `Duration` | Immutability and self-validation |
| **Domain Service** | `RouteDistanceCalculator` | Logic requiring two airport coordinates |
| **Strategy** | `RouteDistanceCalculator` implementations | Allows alternative distance algorithms |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `DuplicateRouteNameException` | Name already exists | Application |
| `AirportNotFoundException` | Airport ID not found | Application |
| `InactiveAirportException` | Airport not operational | Application |
| `IllegalStateException` | ATCC not linked to company | Application |
| `DomainException` | Origin equals destination | Domain |
| `IllegalArgumentException` | Invalid route name format | Domain |

---

## 7. Final Design Considerations

The design prioritises:

- aggregate consistency (origin ≠ destination enforced in domain);
- global route name uniqueness;
- reference by ID to Airport and Company aggregates;
- automatic distance/duration derivation from airport coordinates;
- active status on creation for immediate use in US080.

See also the global aggregate definition at `docs/global-artifacts/Aggregates/09_FlightRoute.puml`.
