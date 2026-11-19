# US082 – Insert Weather Data into a Flight Plan

---

# US Description

As a Pilot, I want to add weather data to a flight plan of mine.

If the flight plan has been previously tested, the test is deemed void because of the new weather data.

The functionality allows a Pilot to associate Weather Data with one of their Flight Plans while ensuring the consistency of flight plan state and aggregate integrity.

---

# Design Decisions

## FlightPlan as Aggregate Root

The design defines `FlightPlan` as the Aggregate Root because:

* the operation modifies the operational state of the flight plan;
* validation consistency (TESTED → PENDING_TEST) belongs to the lifecycle of the FlightPlan;
* Weather Data association changes business-critical state;
* all invariant enforcement must occur atomically.

The Aggregate Root protects:

* ownership validation (pilotId must match);
* state transitions (CANCELLED/COMPLETED rejected, TESTED → PENDING_TEST);
* weather data association per leg;
* operational consistency.

The controller never modifies internal aggregate state directly.

All state changes occur through domain behavior:

* `insertWeatherData(WeatherData, FlightLegId)`
* `updateWeatherData(WindSpeed, WindDirection)`

This preserves encapsulation and aggregate consistency.

---

## WeatherData as Independent Aggregate

`WeatherData` is modeled as a separate Aggregate Root because:

* weather information has its own lifecycle;
* weather records may be reused by multiple flight legs;
* weather information is independently managed and persisted.

The relationship between aggregates is performed using entity references:

* `FlightLeg` has a `@ManyToOne` to `WeatherData`

This allows independent persistence while maintaining referential integrity.

---

## State Invalidation Inside Domain

The rule:

> "If the flight plan has been previously tested, the test is deemed void"

is implemented inside the domain layer.

The application layer does not decide when validation becomes void.

Instead:

* the controller delegates the operation to the domain;
* `FlightPlan.insertWeatherData()` checks status and transitions TESTED → PENDING_TEST.

This follows the DDD principle:

* rich domain model;
* behavior-centered entities.

---

## Repository Abstraction

Repositories are defined per Aggregate Root:

* `FlightPlanRepository`
* `WeatherDataRepository`

Repositories conceptually behave as in-memory collections.

Infrastructure implementations:

* `JpaFlightPlanRepository`
* `JpaWeatherDataRepository`

remain isolated from the domain model.

---

# Architectural Mapping

---

## Presentation Layer

### Classes

* `InsertWeatherDataUI`
* `InsertWeatherDataDTO`

### Responsibilities

The Presentation Layer:

* captures Pilot input (flight plan selection, leg selection, weather data values);
* creates and sends `InsertWeatherDataDTO` to the controller;
* presents success/error messages.

The UI contains no business logic.

---

## Application Layer

### Class

* `InsertWeatherDataInFlightController`

### Responsibilities

The controller coordinates the use case:

* authenticates the pilot via `AuthorizationPort`;
* retrieves repositories via `PersistenceContext`;
* loads `FlightPlan` aggregate;
* validates ownership (pilotId check);
* handles TESTED → PENDING_TEST transition (repeated in domain for safety);
* creates and persists `WeatherData` entity;
* invokes domain behavior via `FlightPlan.insertWeatherData()`;
* persists aggregate changes.

The controller does not:

* contain business rules beyond coordination;
* manipulate aggregate state directly beyond status transition (which is also guarded by domain invariants).

This preserves separation of concerns.

---

## Domain Layer

### Main Elements

#### Aggregate Roots

* `FlightPlan`
* `WeatherData`

#### Entities (within FlightPlan aggregate)

* `FlightLeg`

#### Value Objects

* `FlightPlanId`
* `FlightPlanDesignator`
* `FlightPlanStatus` (enum)
* `FlightType` (enum)
* `DepartureDateTime`
* `FlightWind`
* `WindSpeed`
* `WindDirection`
* `FlightLegId`
* `FuelPlan`
* `FlightSegmentId`
* `PilotId`
* `AirportId`
* `RegistrationNumber`
* `FlightRouteId`
* `AirControlAreaCode`
* `WeatherDataId`
* `WeatherSource`

### Responsibilities

The Domain Layer:

* protects invariants;
* controls state transitions;
* encapsulates business behavior;
* guarantees aggregate consistency.

---

## Infrastructure / Persistence Layer

### Classes

* `JpaFlightPlanRepository`
* `JpaWeatherDataRepository`
* `JpaRepositoryFactory`
* `InMemoryRepositoryFactory`
* `PersistenceContext`
* `FrameworkAuthorizationAdapter`

### Responsibilities

The persistence layer:

* stores aggregates;
* retrieves aggregates;
* isolates technical persistence concerns.

Factories abstract repository instantiation and support:

* relational persistence;
* in-memory persistence.

This aligns with NFR08.

---

# Class Diagram (CD) Analysis

---

## Aggregate Composition

### FlightPlan Aggregate

The `FlightPlan` aggregate contains:

* identity (`FlightPlanId`);
* aircraft registration (`RegistrationNumber`);
* flight route reference (`FlightRouteId`);
* pilot reference (`PilotId`);
* designator (`FlightPlanDesignator`);
* operational status (`FlightPlanStatus`);
* flight type (`FlightType`);
* departure date/time (`DepartureDateTime`);
* legs (`List<FlightLeg>`);
* wind data (`FlightWind`).

Composition relationships (`*--`) indicate strong lifecycle ownership.

Examples:

* `FlightLeg`
* `FlightWind`

These entities/VOs cannot exist independently from `FlightPlan`.

---

## WeatherData Aggregate

`WeatherData` encapsulates:

* identity (`WeatherDataId`);
* air control area code (`AirControlAreaCode`);
* observation timestamp (`observedAt`);
* wind speed (`windSpeedMs`);
* wind direction (`windDirectionDegrees`);
* temperature (`temperatureC`);
* humidity (`humidityPercent`);
* source (`WeatherSource`).

---

## Cross-Aggregate References

The model uses entity/JPA references for cross-aggregate links:

* `FlightLeg` has `@ManyToOne` to `WeatherData`
* `FlightPlan` has `@Embedded` references to `PilotId`, `FlightRouteId`

This allows referential integrity via JPA while maintaining aggregate boundaries.

---

## Multiplicity Analysis

### FlightPlan → FlightLeg

`FlightPlan "1" --> "1..*" FlightLeg`

A FlightPlan must contain at least one leg (invariant enforced in constructor).

---

### FlightLeg → WeatherData

`FlightLeg "1" --> "0..1" WeatherData`

A leg may have at most one WeatherData assignment.

---

## Stereotype Analysis

### <<AggregateRoot>>

Marks consistency boundaries.

### <<Entity>>

Marks domain entity with identity within an aggregate.

### <<ValueObject>>

Represents immutable domain concepts without identity.

### <<Repository>>

Abstracts aggregate persistence.

### <<Controller>>

Coordinates use case execution.

### <<DTO>>

Transfers data across layers.

### <<Factory>>

Encapsulates repository creation logic.

---

# Sequence Diagram (SD) Walkthrough

---

## Step 1 — Pilot Initiates Use Case

The Pilot requests weather insertion through:

* `InsertWeatherDataUI`

The UI creates:

* `InsertWeatherDataInFlightController`

using:

* `AuthorizationPort` (via `FrameworkAuthorizationAdapter.usingRegistry()`)
* `FlightPlanRepository`
* `WeatherDataRepository`

This keeps infrastructure creation abstracted.

---

## Step 2 — Pilot Selects Flight Plan and Leg

The UI displays available flight plans.

The Pilot selects:

* `flightPlanId`
* `legId`

---

## Step 3 — Pilot Provides Weather Data

The UI prompts for weather values:

* `areaCode`
* `observedAt`
* `windSpeedMs`
* `windDirectionDeg`
* `temperatureC`
* `humidityPercent`

The UI creates:

* `InsertWeatherDataDTO`

---

## Step 4 — Controller Authenticates

The controller invokes:

* `ensureAuthenticatedUserHasAnyOf(PILOT)`

through:

* `AuthorizationPort`

If not authorized:

* the operation is aborted.

---

## Step 5 — Load FlightPlan Aggregate

The controller invokes:

* `findById(flightPlanId)`

through:

* `FlightPlanRepository`

If the FlightPlan does not exist:

* `IllegalArgumentException` is thrown.

---

## Step 6 — Ownership Validation

The controller compares:

* `pilotId.equals(flightPlan.getPilot())`

If pilot does not match:

* `IllegalArgumentException` is thrown.

This demonstrates:

# Tell, Don't Ask

The controller asks the aggregate who the owner is rather than manipulating state.

---

## Step 7 — Status Transition (Controller Level)

If the FlightPlan status is `TESTED`:

* the controller sets status to `PENDING_TEST`

This is a pragmatic choice to revert validation before creating weather data.

---

## Step 8 — Create and Persist WeatherData

The controller creates:

* `new WeatherData(areaCode, observedAt, windSpeedMs, windDirectionDeg, temperatureC, humidityPercent)`

The WeatherData entity auto-generates its own `WeatherDataId`.

The controller persists it via:

* `WeatherDataRepository.save(weatherData)`

---

## Step 9 — Domain Method Execution

The controller invokes:

* `flightPlan.insertWeatherData(weatherData, legId)`

The domain method:

1. checks status is not `CANCELLED` or `COMPLETED`;
2. finds the leg by `legId`;
3. transitions `TESTED` → `PENDING_TEST` if needed;
4. calls `leg.assignWeatherData(weatherData)`.

---

## Step 10 — Persist Aggregate

The controller persists the updated FlightPlan:

* `flightPlanRepo.save(flightPlan)`

The persistence layer stores the aggregate atomically.

---

## Step 11 — Return Result

The controller returns normally (void).

The UI informs the Pilot:

* weather data was added;
* previous validation was voided (if applicable).

---

# Invariants Protected by the Design

1. A Pilot may only modify their own FlightPlans.
2. WeatherData is created and persisted before association.
3. A FlightPlan in CANCELLED or COMPLETED state cannot receive new weather data.
4. If status is TESTED, it reverts to PENDING_TEST after weather data insertion.
5. The target leg must exist in the FlightPlan.
6. WeatherData field invariants: windSpeedMs >= 0, windDirectionDeg in [0, 360[, humidityPercent in [0, 100].
7. A FlightPlan must contain at least one leg.
8. Aggregate mutations occur only through domain behavior.

---

# Applied Patterns

## Domain-Driven Design (DDD)

* Aggregate Root
* Entity
* Value Object
* Repository

---

## Repository Pattern

Persistence abstraction per aggregate.

---

## Factory Pattern

Repository instantiation:

* `PersistenceContext.repositories()`
* `JpaRepositoryFactory`
* `InMemoryRepositoryFactory`

---

## DTO Pattern

Data transfer between layers:

* `InsertWeatherDataDTO`

---

## Layered Architecture

Separation between:

* Presentation;
* Application;
* Domain;
* Infrastructure.

---

# Final Design Considerations

The design prioritizes:

* aggregate consistency;
* encapsulation of business rules;
* low coupling between aggregates;
* repository abstraction;
* strict separation of concerns.

The resulting architecture aligns with:

* Domain-Driven Design principles;
* layered architecture;
* transactional consistency;
* maintainable enterprise application design.
