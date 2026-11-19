# US080 — Design

## 1. Overview

This document describes the detailed software design for **US080 — Register a Flight Plan for a Route**, following a **Layered Architecture** with **DDD tactical patterns**.

> The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_080/`.

---

## 2. Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER                    │
│           RegisterFlightPlanUI (Console/API)          │
└───────────────────────┬──────────────────────────────┘
                        │ uses DTO
┌───────────────────────▼──────────────────────────────┐
│                  APPLICATION LAYER                    │
│  RegisterFlightPlanController                         │
│  FlightPlanFactory                                    │
│  FlightPlanDTO                                        │
│  LegDTO                                               │
│  WindDTO                                              │
└──────────┬────────────────────────┬──────────────────┘
           │ uses domain objects    │ calls
┌──────────▼────────────┐  ┌───────▼──────────────────┐
│     DOMAIN LAYER      │  │   REPOSITORY INTERFACES  │
│  FlightPlan           │  │  FlightPlanRepository     │
│  FlightLeg            │  │  FlightRouteRepository    │
│  FlightSegment        │  │  AircraftRepository       │
│  FlightPlanDesignator │  │  PilotRepository          │
│  FlightPlanStatus     │  │  AircraftModelRepository  │
│  DepartureDateTime    │  └───────┬──────────────────┘
│  FuelPlan             │          │ implemented by
│  AltitudeSlot         │  ┌───────▼──────────────────┐
│  FlightWind           │  │  INFRASTRUCTURE LAYER    │
│  FlightType           │  │  JpaFlightPlanRepository │
│  FlightLegId          │  │  InMemoryFlightPlanRepo  │
│  FlightSegmentId      │  └─────────────────────────┘
│  FlightPlanId         │
└───────────────────────┘
```

---

## 3. Class Design

### 3.1 Domain Layer

#### `FlightPlan` — Aggregate Root

```java
public class FlightPlan implements AggregateRoot<FlightPlanId> {

    private FlightPlanId id;
    private FlightPlanDesignator designator;
    private FlightPlanStatus status;
    private DepartureDateTime departureDateTime;
    private FlightType flightType;
    private RegistrationNumber aircraftReg;
    private FlightRouteId flightRouteId;
    private PilotId pilotId;
    private List<FlightLeg> legs;
    private FlightWind wind;

    // Constructor — only valid creation path
    public FlightPlan(FlightPlanId id, RegistrationNumber aircraftRegistration,
                      FlightRouteId flightRouteId, FlightPlanDesignator designator,
                      FlightType flightType, DepartureDateTime departureDateTime,
                      List<FlightLeg> legs, FlightWind wind) {
        this.id = id;
        this.aircraftReg = aircraftRegistration;
        this.flightRouteId = flightRouteId;
        this.designator = designator;
        this.flightType = flightType;
        this.departureDateTime = departureDateTime;
        this.status = FlightPlanStatus.DRAFT;
        this.legs = new ArrayList<>(legs);
        this.wind = wind;
    }

    // Status transition
    public void setStatus(FlightPlanStatus status) {
        this.status = status;
    }

    public void setPilot(PilotId pilotId) {
        this.pilotId = pilotId;
    }
}
```

#### `FlightPlanDesignator` — Value Object

```java
public final class FlightPlanDesignator implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}\\d{4}$");

    private final String value;

    public FlightPlanDesignator(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid FlightPlanDesignator: " + value);
        }
        this.value = value;
    }

    public String value() { return value; }
}
```

#### `FlightPlanStatus` — Enum

```java
public enum FlightPlanStatus {
    DRAFT, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, DELAYED,
    DIVERTED, IN_AIR, LANDED, ON_STANDBY, ON_HOLD, TAKEOFF;
}
```

#### `DepartureDateTime` — Value Object

```java
public final class DepartureDateTime implements ValueObject {

    private final LocalDateTime value;

    public DepartureDateTime(LocalDateTime value) {
        Preconditions.notNull(value, "Departure date/time is mandatory");
        this.value = value;
    }

    public LocalDateTime value() { return value; }
}
```

#### `FlightType` — Enum

```java
public enum FlightType {
    COMMERCIAL, CARGO, CHARTER, PRIVATE, TRAINING, MEDICAL,
    EMERGENCY, MILITARY, OTHER, FIREFIGHTING, AERIAL_WORK,
    AIR_TAXI, AMBULANCE, EXECUTIVE, GOVERNMENT, SCHEDULED,
    UNSCHEDULED, VFR, IFR;
}
```

#### `FlightLeg` — Entity (within FlightPlan aggregate)

```java
public class FlightLeg implements Entity<FlightLegId> {

    private FlightLegId id;
    private int legOrder;
    private Airport origin;
    private Airport destination;
    private double distance;
    private FuelPlan fuelPlan;
    private FlightSegment segment;
    private AltitudeSlot altitudeSlot;
    private int estimatedDuration;
    private int estimatedArrival;
}
```

#### `FlightSegment` — Entity (within FlightLeg)

```java
public class FlightSegment implements Entity<FlightSegmentId> {

    private FlightSegmentId id;
    private Airport origin;
    private Airport destination;
    private double distance;
    private double altitude;
}
```

#### `FlightLegId` — Value Object

```java
public final class FlightLegId implements ValueObject {
    private static final long serialVersionUID = 1L;
    private final UUID id;
    // ...
}
```

#### `FlightSegmentId` — Value Object

```java
public final class FlightSegmentId implements ValueObject {
    private static final long serialVersionUID = 1L;
    private final UUID id;
    // ...
}
```

---

### 3.2 Repository Interface — Domain Layer

```java
public interface FlightPlanRepository
        extends DomainRepository<FlightPlanId, FlightPlan> {

    FlightPlan save(FlightPlan flightPlan);
}
```

---

### 3.3 Application Layer

#### `RegisterFlightPlanController`

```java
public class RegisterFlightPlanController {

    private final AuthorizationService authz;
    private final FlightRouteRepository flightRouteRepo;
    private final AircraftRepository aircraftRepo;
    private final PilotRepository pilotRepo;
    private final AircraftModelRepository aircraftModelRepo;
    private final FlightPlanRepository flightPlanRepo;

    public RegisterFlightPlanController(
            AuthorizationService authz,
            FlightRouteRepository flightRouteRepo,
            AircraftRepository aircraftRepo,
            PilotRepository pilotRepo,
            AircraftModelRepository aircraftModelRepo,
            FlightPlanRepository flightPlanRepo) { ... }

    public List<PilotNameDTO> listPilots() { ... }
    public List<FlightRouteDTO> listFlightRoutes(PilotId pilotId) { ... }
    public List<AircraftDTO> listAircraft(PilotId pilotId) { ... }

    public FlightPlanDTO register(
            PilotId pilotId, FlightRouteId routeId,
            RegistrationNumber aircraftReg,
            String designatorStr, String flightTypeStr,
            String departureDateTimeStr) {

        // 1. Validate designator letters match company IATA code
        // 2. Validate all preconditions via FlightPlanFactory
        // 3. Create FlightPlan, set pilot, persist
        // 4. Return FlightPlanDTO
    }
}
```

#### `FlightPlanFactory`

```java
public final class FlightPlanFactory {

    public static void validatePreconditions(
            FlightPlanDesignator designator,
            FlightRoute route, Aircraft aircraft, Pilot pilot,
            AircraftModel model, List<FlightLeg> legs) {

        // Validate designator letters == company IATA code
        // Validate route is ACTIVE
        // Validate route company == pilot company
        // Validate aircraft is ACTIVE
        // Validate pilot is ACTIVE
        // Validate total fuel <= model max capacity
    }

    public static FlightPlan createFlightPlan(
            FlightPlanDesignator designator,
            FlightType flightType,
            DepartureDateTime departure,
            FlightRoute flightRoute,
            RegistrationNumber aircraftReg,
            List<FlightLeg> legs,
            FlightWind flightWind) { ... }
}
```

#### `FlightPlanDTO` — Output DTO

```java
public record FlightPlanDTO(
    FlightPlanId flightPlanId,
    String designator,
    FlightType flightType,
    DepartureDateTime departureDateTime,
    FlightPlanStatus status,
    FlightRouteId flightRouteId,
    String routeDescription,
    String aircraftRegistration,
    String pilotName,
    String pilotCompany,
    String companyIata,
    LegDTO leg
) {}
```

#### `LegDTO`

```java
public record LegDTO(
    Airport origin,
    Airport destination,
    double distance,
    FuelPlan fuelPlan,
    WindDTO wind
) {}
```

#### `WindDTO`

```java
public record WindDTO(
    double windSpeed,
    int windDirection
) {}
```

---

### 3.4 Infrastructure Layer

#### Abstract Factory (RepositoryFactory pattern)

```java
public interface RepositoryFactory {
    FlightPlanRepository flightPlanRepository();
    FlightRouteRepository flightRouteRepository();
    AircraftRepository aircraftRepository();
    PilotRepository pilotRepository();
    AircraftModelRepository aircraftModelRepository();
}

// JPA implementation (production)
public class JpaRepositoryFactory implements RepositoryFactory {
    public FlightPlanRepository flightPlanRepository() {
        return new JpaFlightPlanRepository();
    }
    // ...
}
```

#### JPA Entity Mapping

```java
@Entity
@Table(name = "FLIGHT_PLAN")
public class FlightPlan {

    @EmbeddedId
    private FlightPlanId id;

    @Embedded
    @AttributeOverride(name = "value",
        column = @Column(name = "DESIGNATOR", nullable = false, unique = true))
    private FlightPlanDesignator designator;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private FlightPlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "FLIGHT_TYPE", nullable = false)
    private FlightType flightType;

    @Embedded
    @AttributeOverride(name = "value",
        column = @Column(name = "DEPARTURE_DATETIME", nullable = false))
    private DepartureDateTime departureDateTime;

    @Embedded
    @AttributeOverride(name = "registration",
        column = @Column(name = "AIRCRAFT_REG", nullable = false))
    private RegistrationNumber aircraftReg;

    @Column(name = "FLIGHT_ROUTE_ID", nullable = false)
    private Long flightRouteId;

    @Column(name = "PILOT_ID")
    private Long pilotId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true,
               mappedBy = "flightPlan")
    private List<FlightLeg> legs = new ArrayList<>();
}
```

---

## 4. Sequence Summary (textual — SD in separate artefact)

```
UI → Controller: register(pilotId, routeId, aircraftReg, designator, flightType, departure)
Controller → Authz: ensureAuthenticatedUserHasAnyOf(PILOT)
Controller → FlightPlanFactory: validatePreconditions(designator, route, aircraft, pilot, model, legs)
Controller → FlightPlanFactory: createFlightPlan(designator, flightType, departure, route, aircraftReg, legs, wind)
Controller → FlightPlan: setPilot(pilotId)
Controller → FlightPlanRepo: save(flightPlan)
Controller → return: FlightPlanDTO
```

---

## 5. Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Factory** | `FlightPlanFactory` | Centralises cross-aggregate validation and object construction |
| **Repository** | `FlightPlanRepository` | Abstracts persistence; collection-like interface |
| **Abstract Factory** | `RepositoryFactory` | Swaps in-memory ↔ JPA via configuration |
| **DTO** | `FlightPlanDTO`, `LegDTO`, `WindDTO` | Decouples domain from presentation |
| **Value Object** | `FlightPlanDesignator`, `DepartureDateTime`, etc. | Immutability; self-validating; no identity |

---

## 6. Error Handling

| Exception | Trigger | Layer |
|-----------|---------|-------|
| `IllegalArgumentException` | Invalid flight designator format | Domain |
| `IllegalArgumentException` | Any null parameter | Domain/Application |
| `IllegalArgumentException` | Missing/empty fields | Application |
| `IllegalArgumentException` | Route not found, inactive, or company mismatch | Application |
| `IllegalArgumentException` | Aircraft not found or inactive | Application |
| `IllegalArgumentException` | Pilot not found or inactive | Application |
| `IllegalArgumentException` | Fuel exceeds model capacity | Application |
| `IllegalArgumentException` | Designator letters != company IATA code | Application |
| `AuthzException` | User not authorised with PILOT role | Application |
