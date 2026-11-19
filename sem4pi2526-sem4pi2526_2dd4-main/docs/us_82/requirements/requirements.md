# US082 – Insert Weather Data in a Flight Plan

## US ID & Description

### US082 – Insert weather data in a flight

**As a Pilot, I want to add weather data to a flight plan of mine.**

Additional rule:

> If the flight plan has been previously tested, the test is deemed void because of the new weather data.

---

# Requirements Analysis

The system must allow a Pilot to insert weather data for a specific leg of an existing flight plan owned by the same pilot.

The weather data includes atmospheric and environmental conditions (wind speed, wind direction, temperature, humidity) relevant for the execution and validation of the flight plan. Since weather conditions directly influence flight simulation and validation, any previous test result becomes invalid after weather data is added.

The operation must ensure:

* the Pilot owns the flight plan;
* the flight plan exists;
* the target leg exists within the flight plan;
* the flight plan is not cancelled or completed;
* the weather data invariants are valid;
* the flight plan status is updated consistently (TESTED → PENDING_TEST).

This functionality belongs primarily to the **Flight Management** bounded context.

---

# Acceptance Criteria

## Functional Criteria

1. The Pilot can select one of their flight plans.
2. The Pilot can select a specific leg within the flight plan.
3. The Pilot can provide weather data (area code, observed at, wind speed, wind direction, temperature, humidity).
4. The system creates and persists the WeatherData entity.
5. The system associates the WeatherData with the selected flight leg.
6. The system sets the flight plan status to PENDING_TEST if it was previously TESTED.
7. The updated flight plan is persisted successfully.
8. The system informs the Pilot of the successful operation.

---

## Validation Criteria

1. A Pilot cannot modify a flight plan belonging to another Pilot.
2. A cancelled or completed flight plan cannot receive new weather data.
3. The target leg must exist in the flight plan.
4. Wind speed must be >= 0.
5. Wind direction must be in [0, 360[ degrees.
6. Humidity must be in [0, 100] percent.
7. The user must be authenticated with the PILOT role.
8. If the flight plan was previously tested, its status must revert to PENDING_TEST automatically.

---

# Domain Model Mapping

## Aggregate Roots

### FlightPlan

Represents the operational plan of a flight.

Contains:

* FlightPlanId (UUID)
* Aircraft (RegistrationNumber)
* FlightRouteId
* PilotId
* FlightPlanDesignator
* FlightPlanStatus (DRAFT, SUBMITTED, APPROVED, REJECTED, ACTIVE, COMPLETED, CANCELLED, PENDING_TEST, TESTED)
* FlightType
* DepartureDateTime
* List of FlightLegs
* FlightWind
* DSL source path

Key domain behavior:

* `insertWeatherData(WeatherData, FlightLegId)` — validates state, finds leg, assigns weather, reverts test status

---

### WeatherData

Represents weather information registered in the system for a specific air control area and observation time.

Contains:

* WeatherDataId (UUID, auto-generated)
* AirControlAreaCode
* observedAt (Instant)
* windSpeedMs (double, >= 0)
* windDirectionDegrees (double, [0, 360[)
* temperatureC (double)
* humidityPercent (double, [0, 100])
* WeatherSource

---

## Entities (within FlightPlan aggregate)

### FlightLeg

Represents a leg of a flight plan, connecting departure and arrival airports.

Contains:

* FlightLegId (UUID)
* DepartureAirport (AirportId)
* ArrivalAirport (AirportId)
* LegOrder
* Segments (List of FlightSegmentId)
* FuelPlan
* WeatherData (nullable, assigned via `assignWeatherData()`)

---

## Value Objects

### FlightPlanId

Unique identifier of a flight plan (UUID).

### FlightPlanDesignator

Human-readable designator for a flight plan (e.g., "FP001").

### FlightLegId

Unique identifier of a flight leg (UUID).

### FlightSegmentId

Unique identifier of a flight segment (UUID).

### DepartureDateTime

The scheduled departure date and time of the flight plan.

### FlightWind

Contains wind speed (WindSpeed) and direction (WindDirection) for the flight plan.

### FuelPlan

Contains total fuel, reserve fuel, and measurement unit.

### AirportId

Unique identifier of an airport.

### AirControlAreaCode

Unique identifier of an air control area (string).

### WeatherDataId

Unique identifier of weather data (UUID, auto-generated).

### WeatherSource

Source of weather data (e.g., "MANUAL_ENTRY", "CSV_DEFAULT").

---

# Aggregate Analysis

## Aggregate Root

### FlightPlan

The `FlightPlan` is the Aggregate Root because:

* weather data association modifies the operational state of the flight plan;
* leg management and validation status consistency must be enforced atomically;
* business rules regarding status transitions belong to the flight plan domain.

The `FlightPlan` aggregate controls:

* association of weather data per leg via `insertWeatherData()`;
* invalidation of previous test results (TESTED → PENDING_TEST);
* consistency of flight operational state.

### WeatherData

The `WeatherData` is also an Aggregate Root because:

* it has its own lifecycle independent of FlightPlan;
* it can be referenced by multiple flight legs over time;
* it is independently persisted.

The relationship is established via JPA `@ManyToOne` from `FlightLeg` to `WeatherData`.

---

# Invariants (Business Rules)

1. A FlightPlan must belong to exactly one Pilot.
2. A Pilot may only modify their own FlightPlans.
3. WeatherData must be created and persisted before association.
4. A FlightPlan in CANCELLED or COMPLETED state cannot be modified.
5. Adding WeatherData to a TESTED flight plan reverts status to PENDING_TEST.
6. The target leg must exist within the FlightPlan.
7. Wind speed must be >= 0.
8. Wind direction must be in [0, 360[ degrees.
9. Humidity must be in [0, 100] percent.
10. The user must be authenticated with the PILOT role before execution.

---

# Layer Responsibilities

## Presentation Layer

Responsibilities:

* expose UI for weather data insertion;
* collect Pilot input (flight plan, leg, weather values);
* construct InsertWeatherDataDTO;
* display success/error messages.

Interface:

* Console UI (InsertWeatherDataUI)

---

## Application Layer

Responsibilities:

* coordinate the use case execution;
* authenticate the user via AuthorizationPort;
* load FlightPlan aggregate;
* validate ownership;
* create and persist WeatherData;
* invoke domain behavior;
* persist changes.

Application controller:

* `InsertWeatherDataInFlightController`

---

## Domain Layer

Responsibilities:

* enforce business invariants;
* manage flight plan state transitions;
* define aggregate consistency rules;
* assign weather data to legs.

Core domain behaviors:

* `FlightPlan.insertWeatherData(WeatherData, FlightLegId)`
* `FlightLeg.assignWeatherData(WeatherData)`

---

## Infrastructure Layer

Responsibilities:

* persistence of aggregates;
* repository implementation;
* database mapping;
* authorization adapter.

Repositories:

* `FlightPlanRepository`
* `WeatherDataRepository`

---

# Pre-conditions

1. The Pilot is authenticated with the PILOT role.
2. The FlightPlan exists.
3. The FlightPlan belongs to the authenticated Pilot.
4. The FlightPlan is not CANCELLED or COMPLETED.
5. The target leg exists within the FlightPlan.

---

# Post-conditions

1. A new WeatherData entity is created and persisted.
2. WeatherData becomes associated with the target FlightLeg.
3. If the FlightPlan was TESTED, its status becomes PENDING_TEST.
4. The updated aggregate is persisted successfully.

---

# Functional Dependencies

This User Story depends directly on:

* US030 – Authentication and authorization
* US080 – Create a flight plan

Indirect dependencies:

* US041 – Register weather data (WeatherData entity creation)
* US085 – Test/validate flight plan (TESTED → PENDING_TEST transition)

---

# Suggested Application Flow

1. Authenticate Pilot (PILOT role required)
2. Load FlightPlan
3. Verify ownership
4. If status is TESTED, set to PENDING_TEST
5. Create WeatherData entity with provided values
6. Persist WeatherData
7. Call FlightPlan.insertWeatherData(WeatherData, FlightLegId)
8. Persist updated FlightPlan
9. Return success

---

# Notes for Future Evolution

Potential future extensions:

* Multiple weather data assignments per leg (historic tracking);
* Automatic weather synchronization from external services;
* Weather forecast versioning;
* Historical weather audit trail;
* Automatic revalidation trigger after weather updates.
