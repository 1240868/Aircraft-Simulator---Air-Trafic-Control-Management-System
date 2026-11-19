# US080 — Analysis

## 1. Overview

This document describes the domain analysis for **US080 — Register a Flight Plan for a Route**.
It identifies the relevant domain concepts, aggregates, value objects, repositories and services following **Domain-Driven Design (DDD)** tactical patterns.

---

## 2. Domain Concepts Identified

| Concept | Type | Justification |
|---------|------|---------------|
| `FlightPlan` | **Aggregate Root (Entity)** | Central concept of this US; has lifecycle and identity |
| `FlightPlanDesignator` | **Value Object** | Immutable, format-constrained identifier (`^[A-Z]{2}\d{4}$`) |
| `FlightPlanStatus` | **Value Object (Enum)** | Represents lifecycle state: `DRAFT`, `CONFIRMED`, `ACTIVE`, etc. |
| `FlightType` | **Value Object (Enum)** | Categorises the flight: `COMMERCIAL`, `CARGO`, `CHARTER`, etc. |
| `DepartureDateTime` | **Value Object** | Validated timestamp for departure |
| `FuelPlan` | **Value Object** | Fuel quantity + unit; embedded in `FlightLeg` |
| `AltitudeSlot` | **Value Object** | Altitude assignment for a leg |
| `FlightWind` | **Value Object** | Wind speed + direction for the flight |
| `FlightLeg` | **Entity (within FlightPlan aggregate)** | One non-stop segment of the flight; owned by FlightPlan |
| `FlightLegId` | **Value Object** | UUID identity for FlightLeg |
| `FlightSegment` | **Entity (within FlightLeg)** | Sub-segment of a leg |
| `FlightSegmentId` | **Value Object** | UUID identity for FlightSegment |
| `FlightRoute` | **Aggregate Root (Entity)** | Pre-existing aggregate; referenced by ID only from FlightPlan |
| `Aircraft` | **Aggregate Root (Entity)** | Pre-existing aggregate; referenced by registration only |
| `Pilot` | **Aggregate Root (Entity)** | Pre-existing aggregate; referenced by ID only |
| `AirTransportCompany` | **Aggregate Root (Entity)** | Owns routes, aircraft and pilots |

---

## 3. Aggregate Design

### 3.1 FlightPlan Aggregate

```
FlightPlan (Aggregate Root)
├── FlightPlanId               (Value Object — UUID identity)
├── FlightPlanDesignator       (Value Object — format-constrained string)
├── FlightPlanStatus           (Value Object — lifecycle enum)
├── FlightType                 (Value Object — flight category enum)
├── DepartureDateTime          (Value Object — validated timestamp)
├── RegistrationNumber         (Value Object — aircraft registration)
├── FlightRouteId              (Reference by ID — Route aggregate)
├── PilotId                    (Reference by ID — Pilot aggregate)
├── FlightWind                 (Value Object — wind speed + direction)
└── List<FlightLeg>            (Entity collection, owned by FlightPlan)
      └── FlightLegId          (Value Object — UUID)
      └── FuelPlan             (Value Object — quantity + unit)
      └── FlightSegment        (Entity)
            └── FlightSegmentId (Value Object — UUID)
      └── AltitudeSlot         (Value Object — altitude assignment)
```

**Invariants enforced by the Aggregate Root:**

| Invariant | Description |
|-----------|-------------|
| INV1 | `FlightPlanDesignator` must match format `^[A-Z]{2}\d{4}$` |
| INV2 | Status on creation must be `DRAFT` |
| INV3 | `DepartureDateTime` must not be null |
| INV4 | `FlightRouteId`, `PilotId` and `RegistrationNumber` must not be null |

**Why FlightPlan is an Aggregate Root:**
A FlightPlan enforces all business rules related to a specific flight execution. It controls transitions of its own status, owns its legs (with segments, fuel and altitude), and is the consistency boundary for departure time. External aggregates (FlightRoute, Aircraft, Pilot) are referenced by ID to respect aggregate boundaries.

---

### 3.2 Cross-Aggregate References

FlightPlan references other aggregates **by identity only** (ID references), never holding direct object references. Domain integrity across aggregates is validated at the **Application** level before creating the FlightPlan.

```
FlightPlan ──(flightRouteId)──► FlightRoute
FlightPlan ──(PilotId)───────► Pilot
FlightPlan ──(Registration)──► Aircraft (via registration number)
```

---

## 4. Value Objects Detail

### FlightPlanDesignator
```
Format: ^[A-Z]{2}\d{4}$
  xx    = airline IATA code (2 uppercase letters must match company IATA)
  dddd  = numeric flight number (exactly 4 digits)

Examples: TP1920, FR1234, BA5678
```
- Validated at construction time
- Immutable
- Equality by value

### FuelPlan
```
attributes:
  - fuelQuantity   : FuelQuantity (amount + unit)
  - fuelType       : FuelType (enum)
```
- Embedded in FlightLeg
- Total fuel must not exceed aircraft model max fuel capacity (checked at Application level)

### FlightPlanStatus (Enum)
```
DRAFT → CONFIRMED → ACTIVE → COMPLETED
                   ↘ CANCELLED
```
- Starts as DRAFT on creation

### FlightType (Enum)
```
COMMERCIAL, CARGO, CHARTER, PRIVATE, TRAINING, MEDICAL,
EMERGENCY, MILITARY, OTHER, FIREFIGHTING, AERIAL_WORK,
AIR_TAXI, AMBULANCE, EXECUTIVE, GOVERNMENT, SCHEDULED,
UNSCHEDULED, VFR, IFR
```

### DepartureDateTime
```
attributes:
  - value : LocalDateTime
```
- Immutable
- Validated at construction: must not be null

---

## 5. Repository Interfaces

Following DDD, **one repository per Aggregate Root**.

### FlightPlanRepository
```java
public interface FlightPlanRepository extends DomainRepository<FlightPlanId, FlightPlan> {
    FlightPlan save(FlightPlan flightPlan);
}
```

### Supporting repositories (pre-existing, consumed by Application)
- `FlightRouteRepository` — verify route existence and active status
- `AircraftRepository` — verify aircraft operational status
- `PilotRepository` — verify pilot status and company membership
- `AircraftModelRepository` — verify max fuel capacity

---

## 6. Application Layer

### 6.1 Controller: `RegisterFlightPlanController`

Orchestrates the use case. Responsibilities:

1. Authorise the requesting user (must have PILOT role)
2. List available pilots
3. List available flight routes (filtered by pilot company)
4. List available aircraft (filtered by pilot company)
5. Validate cross-aggregate preconditions via `FlightPlanFactory.validatePreconditions()`
6. Delegate creation to `FlightPlanFactory.createFlightPlan()`
7. Persist via `FlightPlanRepository`
8. Return a `FlightPlanDTO`

### 6.2 Factory: `FlightPlanFactory`

Static helper that validates all cross-aggregate constraints before constructing a `FlightPlan`:

- Designator first 2 letters match the company IATA code
- Route is ACTIVE
- Route company == pilot company
- Aircraft is ACTIVE
- Pilot is ACTIVE
- Total fuel across all legs <= aircraft model max fuel capacity

---

## 7. DTOs

### FlightPlanDTO (returned to Presentation)
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

### LegDTO
```java
public record LegDTO(
    Airport origin,
    Airport destination,
    double distance,
    FuelPlan fuelPlan,
    WindDTO wind
) {}
```

### WindDTO
```java
public record WindDTO(
    double windSpeed,
    int windDirection
) {}
```

---

## 8. Layer Responsibilities Summary

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `RegisterFlightPlanUI` | Collects input from Pilot, displays result/errors |
| **Application** | `RegisterFlightPlanController` | Orchestrates the use case flow |
| **Application** | `FlightPlanFactory` | Cross-aggregate validation and object construction |
| **Domain** | `FlightPlan` | Enforces invariants, status lifecycle |
| **Domain** | `FlightPlanDesignator`, `FuelPlan`, `FlightPlanStatus`, `DepartureDateTime` | Value object validation |
| **Infrastructure** | `JpaFlightPlanRepository` / `InMemoryFlightPlanRepository` | Persistence implementations |

---

## 9. Key Design Decisions

| Decision | Justification |
|----------|---------------|
| FlightPlan references other aggregates by ID | Preserves aggregate boundaries; avoids cross-aggregate coupling |
| Status starts as DRAFT on creation | Enforced by FlightPlan constructor — invariant cannot be bypassed |
| FuelPlan and AltitudeSlot are Value Objects | They have no identity; defined by their values; immutable |
| FlightPlanFactory is a static utility | Validates all cross-aggregate rules before construction; no state needed |
| Designator is user-provided (not system-generated) | Pilot supplies the designator; factory validates first 2 letters match company IATA |
| Controller performs all coordination | No separate service class; controller uses repos directly |
