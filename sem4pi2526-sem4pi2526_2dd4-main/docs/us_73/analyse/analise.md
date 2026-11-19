# US073 — Analysis

## 1. Overview

This document describes the domain analysis for **US073 — Create a Flight Route**.  
It identifies the relevant domain concepts, aggregates, value objects, repositories and services following **Domain-Driven Design (DDD)** tactical patterns.

> **Note:** The Class Diagram (CD) and Sequence Diagram (SD) are maintained as separate artefacts in `/docs/us_73/design/`.

---

## 2. Domain Concepts Identified

| Concept | Type | Justification |
|---------|------|---------------|
| `FlightRoute` | **Aggregate Root (Entity)** | Central concept; owns route identity and invariants |
| `FlightRouteName` | **Value Object** | Format-constrained name (`xx` + 1–4 digits) |
| `FlightRouteId` | **Value Object** | Unique aggregate identity |
| `RouteActivationStatus` | **Value Object** | Active/deactivated lifecycle |
| `Distance` | **Value Object** | Geographic distance between airports |
| `Duration` | **Value Object** | Estimated flight time |
| `Airport` | **Aggregate Root (Entity)** | Pre-existing; referenced by ID |
| `AirTransportCompany` | **Aggregate Root (Entity)** | Pre-existing; owns routes |
| `RouteDistanceCalculator` | **Domain Service** | Haversine distance from coordinates |

---

## 3. Aggregate Design

### 3.1 FlightRoute Aggregate

```
FlightRoute (Aggregate Root)
├── FlightRouteId              (identity)
├── FlightRouteName            (Value Object — business identity)
├── CompanyId                  (Reference by ID)
├── originAirportId            (Reference by ID)
├── destinationAirportId     (Reference by ID)
├── RouteActivationStatus      (Value Object — active on creation)
├── Distance                   (Value Object — computed)
└── Duration                   (Value Object — computed)
```

**Invariants enforced by the Aggregate Root:**

| Invariant | Description |
|-----------|-------------|
| INV1 | `FlightRouteName` must match format `^[A-Z]{2}\d{1,4}$` |
| INV2 | `originAirportId` ≠ `destinationAirportId` |
| INV3 | `companyId`, both airport IDs must not be null |
| INV4 | Status on creation is **active** |
| INV5 | `Distance` and `Duration` must be positive after calculation |

**Why FlightRoute is an Aggregate Root:**  
A flight route enforces geographic and naming consistency. It is the consistency boundary for origin/destination rules and route lifecycle. External aggregates (Airport, Company) are referenced by ID.

---

### 3.2 Cross-Aggregate References

```
FlightRoute ──(companyId)──────────► AirTransportCompany
FlightRoute ──(originAirportId)────► Airport
FlightRoute ──(destinationAirportId)► Airport
```

Domain integrity across aggregates is validated at the **Application Service** / **Domain Service** level before creating the FlightRoute.

---

## 4. Value Objects Detail

### FlightRouteName
```
Format: xxn(n)(n)(n)
  xx   = company IATA designator (2 uppercase letters)
  n+   = numeric suffix (1 to 4 digits)

Examples: TP123, TP1234, RY9
```
- Validated at construction time
- Immutable
- Globally unique (checked via repository at Application Service level)

### Distance
```
attributes:
  - value : double  (strictly > 0)
  - unit  : String  (e.g. "km" or "NM")
```

### Duration
```
attributes:
  - value : double  (estimated minutes or hours, > 0)
```

### RouteActivationStatus
```
attributes:
  - isActive    : boolean  (true on creation)
  - activeUntil : LocalDate (optional, for US074)
```

---

## 5. Repository Interfaces

### IFlightRouteRepository
```java
public interface IFlightRouteRepository extends DomainRepository<FlightRouteId, FlightRoute> {
    Optional<FlightRoute> findByName(FlightRouteName name);
    List<FlightRoute> findByCompanyId(CompanyId companyId);
    boolean existsByName(FlightRouteName name);
    FlightRoute save(FlightRoute route);
}
```

### Supporting repositories (pre-existing)
- `IAirportRepository` — verify airport existence and operational status
- `IAirTransportCompanyRepository` — resolve ATCC's company and IATA code

---

## 6. Services

### 6.1 Application Service: `CreateFlightRouteService`

Coordinates the use case:

1. Authenticate and authorise the ATCC
2. Resolve the ATCC's `CompanyId` and company IATA code
3. Build `FlightRouteName` from IATA prefix + numeric suffix
4. Verify name uniqueness via `IFlightRouteRepository`
5. Load and validate origin and destination airports (exist, operational)
6. Delegate distance/duration calculation to `RouteDistanceCalculator`
7. Create `FlightRoute` via factory method
8. Persist via `IFlightRouteRepository`
9. Return `FlightRouteDTO`

### 6.2 Domain Service: `RouteDistanceCalculator`

Computes distance and duration from two airport coordinates:

```java
public interface RouteDistanceCalculator {
    Distance calculateDistance(Coordinates origin, Coordinates destination);
    Duration estimateDuration(Distance distance, double cruiseSpeedKmh);
}
```

This logic does not belong naturally to a single entity (requires two airports), so it is extracted into a Domain Service.

---

## 7. DTO

### FlightRouteDTO
```java
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

## 8. Layer Responsibilities Summary

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `CreateFlightRouteUI` | Collect numeric suffix and airport selection |
| **Application** | `CreateFlightRouteController` | Orchestrates use case, resolves company |
| **Application** | `CreateFlightRouteService` | Cross-aggregate validation, creation coordination |
| **Domain** | `FlightRoute` | Enforces invariants, lifecycle |
| **Domain** | `FlightRouteName`, `Distance`, `Duration` | Value object validation |
| **Domain** | `RouteDistanceCalculator` | Geographic computation |
| **Infrastructure** | `FlightRouteJpaRepository` | Persistence |

---

## 9. Key Design Decisions

| Decision | Justification |
|----------|---------------|
| FlightRoute references airports/company by ID | Preserves aggregate boundaries |
| Name prefix derived from company IATA | Matches project specification (2 company letters) |
| Global name uniqueness | Explicit project requirement |
| Origin ≠ destination enforced in aggregate | Core business invariant |
| Distance calculator as Domain Service | Requires coordinates from two external aggregates |
| Active status on creation | Routes are immediately usable for flight plans |

---

## 10. Functional Dependencies

| US | Relationship |
|----|-------------|
| US052 | Airports must exist |
| US060 | Company must exist |
| US061 | ATCC must be linked to company |
| US074 | Route deactivation |
| US080 | Flight plan creation consumes routes |
| US030 | Authentication and authorisation |
