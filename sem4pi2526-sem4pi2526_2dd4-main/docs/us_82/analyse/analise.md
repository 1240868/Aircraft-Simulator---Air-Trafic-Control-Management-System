# US082 – Insert weather data in a flight

## US Description

As a Pilot, I want to add weather data to a flight plan of mine.

If the flight plan has been previously tested, the test is deemed void because of the new weather data.

---

# Requirements Analysis

## Objective

The system must allow a Pilot to insert Weather Data into a specific leg of an existing Flight Plan owned by that Pilot.

The Weather Data includes wind speed, wind direction, temperature, and humidity measured at a specific time and air control area. This data is relevant for flight validation and simulation because environmental conditions influence flight execution and safety validation.

Whenever new Weather Data is inserted into a Flight Plan:

* a new WeatherData entity is created and persisted;
* the Flight Leg receives the WeatherData reference;
* if the Flight Plan was previously tested (TESTED status), it reverts to PENDING_TEST, meaning the previous test is deemed void.

---

## Main Challenges Identified

### Ownership Validation

The system must guarantee that a Pilot can only modify Flight Plans belonging to that Pilot.

---

### Consistency of Validation State

A previously tested Flight Plan cannot remain valid after Weather Data changes.

The system must automatically revert TESTED → PENDING_TEST.

---

### Weather Data Invariants

The Weather Data must:

* have wind speed >= 0;
* have wind direction in [0, 360[ degrees;
* have humidity in [0, 100] percent;
* be associated with a valid Air Control Area code;
* have a valid observation timestamp.

---

### Aggregate Consistency

The operation modifies the Flight Plan state and creates a new WeatherData entity.

The Flight Plan aggregate must be saved atomically after insertWeatherData() completes.

---

# Glossary

## Flight Plan

Operational description of a flight, including aircraft, route, legs, fuel, and operational data.

---

## Flight Leg

A segment of a flight plan connecting a departure airport to an arrival airport. Each leg can have associated weather data.

---

## Weather Data

Environmental and atmospheric information associated with an Air Control Area and observation time. Contains wind speed, wind direction, temperature, and humidity.

---

## Validation/Test

Process used to verify if a Flight Plan is operationally valid and safe. The TESTED status indicates a successful test. Inserting weather data reverts this to PENDING_TEST.

---

## Air Control Area

Controlled geographic region where weather conditions and flight operations are monitored, identified by AirControlAreaCode.

---

# Domain Concepts Identification

## Aggregate Roots

### FlightPlan

Represents the operational planning of a flight.

#### Key Attributes

* FlightPlanId (UUID, embedded ID)
* Aircraft (RegistrationNumber)
* FlightRouteId
* PilotId
* FlightPlanDesignator (e.g., "FP001")
* FlightPlanStatus (DRAFT, SUBMITTED, APPROVED, REJECTED, ACTIVE, COMPLETED, CANCELLED, PENDING_TEST, TESTED)
* FlightType (COMMERCIAL, PRIVATE, CARGO)
* DepartureDateTime
* List of FlightLegs
* FlightWind (wind speed + direction)
* DSL source path

#### Key Behavior

* `insertWeatherData(WeatherData, FlightLegId)` — validates state, finds leg, assigns weather, reverts test status

---

### WeatherData

Represents weather information registered in the system.

#### Key Attributes

* WeatherDataId (UUID, auto-generated)
* AirControlAreaCode
* observedAt (Instant)
* windSpeedMs (double)
* windDirectionDegrees (double)
* temperatureC (double)
* humidityPercent (double)
* WeatherSource

---

## Entities (within FlightPlan aggregate)

### FlightLeg

Represents one leg of a flight plan.

#### Key Attributes

* FlightLegId (UUID, embedded ID)
* DepartureAirport (AirportId)
* ArrivalAirport (AirportId)
* LegOrder (int)
* Segments (List of FlightSegmentId)
* FuelPlan
* WeatherData (nullable, @ManyToOne reference)

#### Key Behavior

* `assignWeatherData(WeatherData)` — assigns weather data to the leg

---

## Value Objects (VOs)

### FlightPlanId

Unique identifier of a Flight Plan (UUID).

### FlightPlanDesignator

Human-readable designator (e.g., "FP001").

### FlightLegId

Unique identifier of a Flight Leg (UUID).

### FlightSegmentId

Unique identifier of a Flight Segment (UUID).

### DepartureDateTime

Scheduled departure date and time.

### FlightWind

Wind speed (WindSpeed) and direction (WindDirection) for the flight plan.

### WindSpeed

Wind speed value with unit.

### WindDirection

Wind direction angle relative to North.

### FuelPlan

Total fuel, reserve fuel, and measurement unit.

### AirportId

Unique identifier of an airport.

### AirControlAreaCode

Unique identifier of an Air Control Area (string).

### WeatherDataId

Unique identifier of Weather Data (UUID, auto-generated).

### WeatherSource

Source of weather data (e.g., manual entry, CSV import).

---

## Enums

### FlightPlanStatus

* DRAFT, SUBMITTED, APPROVED, REJECTED, ACTIVE, COMPLETED, CANCELLED, PENDING_TEST, TESTED

### FlightType

* COMMERCIAL, PRIVATE, CARGO

---

# Aggregate Definition

## Primary Aggregate Root

### FlightPlan

The FlightPlan is the primary Aggregate Root because:

* Weather Data association changes Flight Plan operational state;
* validation consistency rules (TESTED → PENDING_TEST) belong to the Flight Plan lifecycle;
* all modifications must preserve Flight Plan invariants atomically.

The Aggregate controls:

* Weather Data association per leg;
* validation invalidation;
* operational consistency.

---

## Secondary Aggregate Root

### WeatherData

The WeatherData is a separate Aggregate Root because:

* it has its own lifecycle and identity;
* it may be referenced by multiple flight legs over time;
* it is independently persisted and managed.

---

# Business Rules & Invariants

## Ownership Rules

1. A Pilot may only modify Flight Plans owned by that Pilot.

---

## State Rules

2. A FlightPlan in CANCELLED or COMPLETED state cannot receive new weather data.

3. If the FlightPlan status is TESTED, inserting weather data reverts it to PENDING_TEST.

---

## Leg Rules

4. The target leg must exist in the FlightPlan.

---

## Weather Data Invariants

5. Wind speed must be >= 0.
6. Wind direction must be in [0, 360[ degrees.
7. Temperature has no restrictions (can be any double).
8. Humidity must be in [0, 100] percent.

---

## Authentication Rules

9. The user must be authenticated with the PILOT role.

---

## Coherence Rules

10. The AirControlArea code must exist in the system.
11. The AirControlArea must match at least one airport (route origin, route destination, or any leg airport) of the flight plan.
12. The observation date must be within ±1 day of the flight departure date.

---

# Responsibilities by Layer

## Application Layer

### Use Case Controller

`InsertWeatherDataInFlightController`

### Responsibilities

* authenticate Pilot via AuthorizationPort;
* validate authorization (PILOT role);
* load FlightPlan;
* validate ownership;
* list flight plans for the current pilot (excluding CANCELLED/COMPLETED);
* list legs for a given flight plan;
* **validate coherence** — ensure the provided AirControlArea code exists, the area matches at least one airport along the flight plan route or legs, and the observation date is within ±1 day of the flight departure;
* handle TESTED → PENDING_TEST status transition;
* create and persist WeatherData;
* invoke domain behavior via FlightPlan.insertWeatherData();
* persist updated aggregate.

The controller must not contain business rules beyond coordination.

---

## Domain Layer

### Responsibilities

* enforce invariants;
* validate state transitions (CANCELLED/COMPLETED rejection);
* revert TESTED → PENDING_TEST;
* find leg and assign weather data;
* maintain aggregate consistency.

### Core Domain Behaviors

* `FlightPlan.insertWeatherData(WeatherData, FlightLegId)`
* `FlightLeg.assignWeatherData(WeatherData)`

Business rules must reside inside the Aggregate Root and domain objects.

---

## Persistence Layer

### Repositories

* `FlightPlanRepository` — retrieve and persist FlightPlan aggregates
* `WeatherDataRepository` — persist WeatherData entities
* `FlightRouteRepository` — retrieve FlightRoute for coherence validation
* `AirportRepository` — retrieve Airport by ICAO code for area matching
* `AirControlAreaRepository` — verify AirControlArea code exists
* `PilotRepository` — resolve PilotId from authenticated user session

### Responsibilities

* retrieve FlightPlan aggregates by ID;
* persist WeatherData entities;
* persist aggregate changes;
* support coherence validation queries (route, airport, area, pilot).

---

# Acceptance Criteria

## Functional Acceptance Criteria

1. The Pilot can select one of their flight plans.
2. The Pilot can select a specific leg within the flight plan.
3. The Pilot can insert weather data (area code, observed at, wind speed, wind direction, temperature, humidity).
4. The system creates and persists the WeatherData successfully.
5. The system assigns the WeatherData to the selected leg.
6. The system reverts TESTED → PENDING_TEST if applicable.
7. The updated FlightPlan is persisted successfully.
8. The system informs the Pilot that the operation succeeded.

---

## Validation Acceptance Criteria

9. The operation fails if the FlightPlan does not belong to the Pilot.
10. The operation fails if the FlightPlan is CANCELLED or COMPLETED.
11. The operation fails if the target leg does not exist.
12. The operation fails if wind speed is negative.
13. The operation fails if wind direction is outside [0, 360[.
14. The operation fails if humidity is outside [0, 100].
15. The operation fails if authentication is invalid.
16. The operation fails if the AirControlArea code does not exist.
17. The operation fails if the AirControlArea does not match any airport along the flight plan route or legs.
18. The operation fails if the observation date is not within ±1 day of the flight departure.

---

# Functional Dependencies

## Direct Dependencies

### US030 – Authentication and authorization

Authentication and authorization (PILOT role) are required before execution.

---

### US080 – Create a flight plan

A Flight Plan must exist before weather data can be added.

---

## Indirect Dependencies

### US085 – Test/validate flight plan

Validation state management (TESTED → PENDING_TEST) depends on the Flight Plan testing lifecycle.

---

# Application Flow

1. Authenticate Pilot (PILOT role)
2. List available flight plans for the current pilot (not CANCELLED nor COMPLETED)
3. Pilot selects a flight plan
4. List legs for the selected flight plan
5. Pilot selects a leg
6. Pilot provides weather data (area code, timestamp, wind, temperature, humidity)
7. Build InsertWeatherDataDTO
8. Controller.insertWeatherData(dto, pilotId):
   a. Re-authenticate Pilot (PILOT role)
   b. Load FlightPlan by ID
   c. Verify ownership
   d. Validate coherence:
      - AirControlArea code exists in repository
      - FlightRoute exists for the flight plan
      - At least one airport (route origin, route destination, or leg airports) belongs to the given area
      - Observation date is within ±1 day of flight departure
   e. If status is TESTED, set to PENDING_TEST
   f. Create WeatherData entity with provided values
   g. Persist WeatherData
   h. Call FlightPlan.insertWeatherData(WeatherData, FlightLegId)
   i. Persist updated FlightPlan
9. Return success
