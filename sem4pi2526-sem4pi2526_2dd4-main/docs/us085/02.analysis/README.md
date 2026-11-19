# US085 — Analysis

## 1. Analysis Process

The analysis identifies all domain concepts, application services, repositories, and infrastructure components required to implement the test/validate flight plan use case. The main challenges are:

1. **Cross-aggregate coordination** — the use case spans FlightPlan, Aircraft, AircraftModel, Airport, AirControlArea, and Pilot aggregates.
2. **C integration** — the physics validator is a standalone C program that must be invoked, fed JSON data, and have its output parsed.
3. **DSL re-validation** — the flight plan's DSL source must be re-parsed for both validation and profile extraction.
4. **Status management** — the FlightPlan status must transition correctly: DRAFT/PENDING_TEST/REJECTED → TESTED (pass) or REJECTED (fail).

---

## 2. Analysis Class Diagram

The following diagram shows ALL classes involved in the US085 solution, spanning domain, application, and infrastructure layers.

```plantuml
@startuml US085_Analysis_CD
title US085 — Analysis Class Diagram (All Layers)

skinparam classAttributeIconSize 0
hide empty members

' ====================================================================
' PRESENTATION LAYER
' ====================================================================
package "Presentation" #EEF0FB {
    class TestFlightPlanUI {
        - controller : TestFlightPlanController
        + doShow() : boolean
        + headline() : String
    }
}

' ====================================================================
' APPLICATION LAYER
' ====================================================================
package "Application" #E8F5F0 {
    class TestFlightPlanController <<(C,#F4A460) Controller>> {
        - authz : AuthorizationPort
        - flightPlanRepo : FlightPlanRepository
        - pilotRepo : PilotRepository
        - dslValidator : FlightPlanDslValidationPort
        - payloadWriter : FlightPlanTestPayloadWriter
        - physicsTester : FlightPlanPhysicsTestPort
        + listTestablePlansForCurrentPilot() : List<FlightPlan>
        + testForCurrentPilot(flightPlanId : FlightPlanId) : FlightPlanTestResult
        + test(flightPlanId : FlightPlanId, pilotId : PilotId) : FlightPlanTestResult
        + isTestable(status : FlightPlanStatus) : boolean {static}
    }

    interface FlightPlanDslValidationPort {
        + validate(flightPlan : FlightPlan) : List<String>
    }

    class FlightPlanDslFileValidator {
        - importer : FlightPlanImporter
        + validate(flightPlan : FlightPlan) : List<String>
    }

    interface FlightPlanTestPayloadWriter {
        + writePayload(flightPlan : FlightPlan) : Path
    }

    interface FlightPlanPhysicsTestPort {
        + run(inputJson : Path) : List<String>
    }

    class FlightPlanPhysicsValidator {
        - validatorBaseDir : Path
        + run(inputJson : Path) : List<String>
    }

    class FlightPlanTestPayloadBuilder {
        - aircraftRepo : AircraftRepository
        - modelRepo : AircraftModelRepository
        - airportRepo : AirportRepository
        - profileReader : DslSegmentProfileReader
        + writePayload(flightPlan : FlightPlan) : Path
        - totalFuelKg(plan : FlightPlan) : double
        - resolveAirport(airportId : AirportId) : Airport
    }

    class DslSegmentProfileReader {
        - importer : FlightPlanImporter
        + readSegmentAltitudesMeters(plan : FlightPlan) : List<Double>
        + toMeters(value : double, unit : String) : double {static}
    }

    record FlightPlanTestResult <<DTO>> {
        + passed : boolean
        + dslErrors : List<String>
        + physicsErrors : List<String>
        + newStatus : FlightPlanStatus
        + success() : FlightPlanTestResult {static}
        + failed(dslErrors, physicsErrors, status) : FlightPlanTestResult {static}
    }

    interface AuthorizationPort {
        + ensureAuthenticatedUserHasAnyOf(roles : Role[]) : void
        + currentUsername() : String
    }

    class FrameworkAuthorizationAdapter {
        + usingRegistry() : AuthorizationPort {static}
    }
}

' ====================================================================
' DOMAIN LAYER
' ====================================================================
package "Domain" #FFF8EC {

    package "FlightPlan Aggregate" {
        class FlightPlan <<(R,#FFCC66) Root>> {
            - id : FlightPlanId
            - designator : FlightPlanDesignator
            - status : FlightPlanStatus
            - flightType : FlightType
            - departureDateTime : DepartureDateTime
            - aircraft : RegistrationNumber
            - flightRoute : FlightRouteId
            - pilot : PilotId
            - legs : List<FlightLeg>
            - wind : FlightWind
            - dslSourcePath : String
            + markTestPassed() : void
            + markTestFailed() : void
            + getStatus() : FlightPlanStatus
            + setStatus(status : FlightPlanStatus) : void
            + getDslSourcePath() : String
            + setDslSourcePath(path : String) : void
            + getPilot() : PilotId
            + getAircraft() : RegistrationNumber
            + getLegs() : List<FlightLeg>
        }
        class FlightPlanId <<(V,#9ACD32) Value Object>>
        class FlightPlanDesignator <<(V,#9ACD32) Value Object>>
        enum FlightPlanStatus {
            DRAFT
            SUBMITTED
            APPROVED
            REJECTED
            ACTIVE
            COMPLETED
            CANCELLED
            PENDING_TEST
            TESTED
        }
        class FlightLeg <<(E,#B0C4DE) Entity>> {
            - id : FlightLegId
            - departureAirport : AirportId
            - arrivalAirport : AirportId
            - legOrder : int
            - segments : List<FlightSegmentId>
            - fuelPlan : FuelPlan
            + fuelPlan() : FuelPlan
            + arrivalAirport() : AirportId
        }
        class FuelPlan <<(V,#9ACD32) Value Object>> {
            - totalFuel : double
            - reserveFuel : double
            - unit : MeasurementUnit
            + totalFuel() : double
            + reserveFuel() : double
        }
        class FlightSegmentId <<(V,#9ACD32) Value Object>>
        class FlightLegId <<(V,#9ACD32) Value Object>>
    }

    package "Aircraft Aggregate" {
        class Aircraft <<(R,#FFCC66) Root>> {
            - registrationNumber : RegistrationNumber
            - aircraftModelId : AircraftModelId
            - ownerCompanyId : CompanyId
            - operationalStatus : AircraftOperationalStatus
            + aircraftModelId() : AircraftModelId
        }
        class RegistrationNumber <<(V,#9ACD32) VO>>
        class AircraftModel <<(R,#FFCC66) Root>> {
            - emptyWeightKg : double
            - mtowKg : double
            - mzfwKg : double
            - maxFuelCapacityLiters : double
            - serviceCeilingM : double
            - cruiseSpeedMs : double
            - wingAreaM2 : double
            - cd0 : double
            - cl : double
            - certifiedEngineModelIds : List<String>
            + serviceCeilingM() : double
            + cruiseSpeedMs() : double
            + wingAreaM2() : double
            + cd0() : double
            + cl() : double
            + emptyWeightKg() : double
        }
        class EngineModel <<(R,#FFCC66) Root>> {
            - id : EngineModelId
            - type : EngineType
            - fuelSpecs : FuelSpecs
            - jetSpecs : JetEngineSpecs
        }
    }

    package "Airport / AirControl" {
        class Airport <<(R,#FFCC66) Root>> {
            - icaoCode : String
            - latitude : double
            - longitude : double
            - elevationMeters : double
            - airControlArea : AirControlArea
            + elevationMeters() : double
        }
        class AirControlArea <<(R,#FFCC66) Root>> {
            - latMin, latMax, lonMin, lonMax : double
        }
        class AirportId <<(V,#9ACD32) VO>>
    }

    package "Pilot" {
        class Pilot <<(R,#FFCC66) Root>> {
            - id : PilotId
            - company : CompanyId
            - email : Email
            + identity() : PilotId
        }
        class PilotId <<(V,#9ACD32) VO>>
    }

    ' =================== REPOSITORIES ===================
    interface FlightPlanRepository <<(I,#DDA0DD) Repository>> {
        + findById(id : FlightPlanId) : Optional<FlightPlan>
        + save(flightPlan : FlightPlan) : FlightPlan
        + findAll() : Iterable<FlightPlan>
        + ofIdentity(id : FlightPlanId) : Optional<FlightPlan>
        + findAvailable()
    }

    interface AircraftRepository <<(I,#DDA0DD) Repository>> {
        + ofIdentity(id : RegistrationNumber) : Optional<Aircraft>
    }

    interface AircraftModelRepository <<(I,#DDA0DD) Repository>> {
        + ofIdentity(id : AircraftModelId) : Optional<AircraftModel>
    }

    interface AirportRepository <<(I,#DDA0DD) Repository>> {
        + findById(code : String) : Optional<Airport>
    }

    interface PilotRepository <<(I,#DDA0DD) Repository>> {
        + findByEmail(email : Email) : Optional<Pilot>
    }
}

' ====================================================================
' INFRASTRUCTURE LAYER
' ====================================================================
package "Infrastructure" #EAF3FB {
    class JpaFlightPlanRepository <<Repository>>
    class JpaAircraftRepository <<Repository>>
    class JpaAircraftModelRepository <<Repository>>
    class JpaAirportRepository <<Repository>>
    class JpaPilotRepository <<Repository>>

    class PersistenceContext {
        + repositories() : RepositoryFactory {static}
    }

    interface RepositoryFactory <<AbstractFactory>> {
        + flightPlans() : FlightPlanRepository
        + pilots() : PilotRepository
        + aircraft() : AircraftRepository
        + aircraftModels() : AircraftModelRepository
        + airports() : AirportRepository
    }
}

' ====================================================================
' EXTERNAL C COMPONENT
' ====================================================================
package "C Validator (lapr.base)" #FFF0F0 {
    class "flight_plan_validator.exe" as CEXE <<executable>> {
        + main(argc, argv) : int
        + run_flight_plan_test(json_path) : int
        - validate_fuel_on_board(json) : int
        - validate_segment_altitudes(json) : int
        - read_payload_file(filename) : char*
        - extract_number(json, key) : double
    }

    note right of CEXE
        **Exit codes:**
        0 = PASS
        1 = FAIL (fuel or altitude)
        2 = Input error

        **Stdout:**
        "PASS" on success
        "FAIL|CODE|message" on failure
    end note

    file "input.json" as JSON <<temp>> {
        flightPlanId
        fuelOnBoardKg
        reserveFuelKg
        minFuelKg
        serviceCeilingM
        minAltitudeM
        segmentAltitudesM[]
    }
}

' ====================================================================
' RELATIONS — Presentation → Application
' ====================================================================
TestFlightPlanUI ..> TestFlightPlanController : uses

' ====================================================================
' RELATIONS — Application → Domain / Ports
' ====================================================================
TestFlightPlanController ..> AuthorizationPort : <<uses>>
TestFlightPlanController ..> FlightPlanRepository : <<uses>>
TestFlightPlanController ..> PilotRepository : <<uses>>
TestFlightPlanController ..> FlightPlanDslValidationPort : <<uses>>
TestFlightPlanController ..> FlightPlanTestPayloadWriter : <<uses>>
TestFlightPlanController ..> FlightPlanPhysicsTestPort : <<uses>>
TestFlightPlanController ..> FlightPlan : <<creates / updates>>
TestFlightPlanController ..> FlightPlanTestResult : <<produces>>

FlightPlanDslFileValidator ..|> FlightPlanDslValidationPort
FlightPlanTestPayloadBuilder ..|> FlightPlanTestPayloadWriter
FlightPlanPhysicsValidator ..|> FlightPlanPhysicsTestPort

FlightPlanDslFileValidator ..> FlightPlan : <<reads DSL path>>
DslSegmentProfileReader ..> FlightPlan : <<reads DSL path>>

FlightPlanTestPayloadBuilder ..> AircraftRepository : <<uses>>
FlightPlanTestPayloadBuilder ..> AircraftModelRepository : <<uses>>
FlightPlanTestPayloadBuilder ..> AirportRepository : <<uses>>
FlightPlanTestPayloadBuilder ..> DslSegmentProfileReader : <<uses>>

' ====================================================================
' RELATIONS — C integration
' ====================================================================
FlightPlanPhysicsValidator ..> CEXE : <<ProcessBuilder>>
FlightPlanTestPayloadBuilder ..> JSON : <<writes>>
CEXE ..> JSON : <<reads>>
CEXE --> CEXE : stdout "PASS"/"FAIL|..."

' ====================================================================
' RELATIONS — Domain composition
' ====================================================================
FlightPlan "1" *-- "1" FlightPlanId
FlightPlan "1" *-- "1" FlightPlanDesignator
FlightPlan "1" *-- "1" FlightPlanStatus
FlightPlan "1" *-- "1..*" FlightLeg
FlightLeg "1" *-- "1" FuelPlan
FlightPlan "*" --> "1" RegistrationNumber : aircraft (ref)
FlightPlan "*" --> "1" PilotId : pilot (ref)
Aircraft "*" --> "1" AircraftModel : model (ref)

' ====================================================================
' RELATIONS — Repositories → Domain
' ====================================================================
FlightPlanRepository ..> FlightPlan : <<manages>>
AircraftRepository ..> Aircraft : <<manages>>
AircraftModelRepository ..> AircraftModel : <<manages>>
AirportRepository ..> Airport : <<manages>>
PilotRepository ..> Pilot : <<manages>>

' ====================================================================
' RELATIONS — Infrastructure → Repositories
' ====================================================================
JpaFlightPlanRepository ..|> FlightPlanRepository
JpaAircraftRepository ..|> AircraftRepository
JpaAircraftModelRepository ..|> AircraftModelRepository
JpaAirportRepository ..|> AirportRepository
JpaPilotRepository ..|> PilotRepository

PersistenceContext --> RepositoryFactory : <<provides>>
RepositoryFactory --> FlightPlanRepository
RepositoryFactory --> PilotRepository
RepositoryFactory --> AircraftRepository
RepositoryFactory --> AircraftModelRepository
RepositoryFactory --> AirportRepository

@enduml
```

---

## 3. C Component Integration Details

### 3.1 The Exchange Protocol

```
┌─────────────────────────┐       write JSON        ┌──────────────────────┐
│                         │ ───────────────────────► │                      │
│  FlightPlanTestPayload  │                          │  temp file           │
│  Builder (Java)         │                          │  fp-test-xxxxx.json  │
│                         │ ◄─────────────────────── │                      │
└─────────────────────────┘       returns Path       └──────────────────────┘
                                                              │
                                                              │ read
                                                              ▼
┌─────────────────────────┐       ProcessBuilder      ┌──────────────────────┐
│                         │ ───────────────────────► │                      │
│  FlightPlanPhysics      │   ./flight_plan_validator │  flight_validator.c │
│  Validator (Java)       │      <input.json>         │  (C program)        │
│                         │ ◄─────────────────────── │                      │
│                         │      stdout               │  exit 0 = PASS      │
│                         │      "PASS" or            │  exit 1 = FAIL      │
│                         │      "FAIL|code|msg"      │  exit 2 = INPUT ERR │
└─────────────────────────┘                           └──────────────────────┘
```

### 3.2 Input JSON Format

The `FlightPlanTestPayloadBuilder` produces the following JSON structure:

```json
{
  "flightPlanId": "uuid-string",
  "fuelOnBoardKg": 12000.00,
  "reserveFuelKg": 2000.00,
  "minFuelKg": 2000.00,
  "serviceCeilingM": 12500.00,
  "minAltitudeM": 150.00,
  "segmentAltitudesM": [10000.00, 9500.00, 11000.00]
}
```

| Field | Source | Description |
|-------|--------|-------------|
| `flightPlanId` | FlightPlan.getId() | Identifies the flight plan being tested |
| `fuelOnBoardKg` | Sum of all legs' `FuelPlan.totalFuel()` | Total fuel loaded (kg) |
| `reserveFuelKg` | Last leg's `FuelPlan.reserveFuel()` | Fuel reserved for contingencies |
| `minFuelKg` | Currently equals `reserveFuelKg` | Minimum fuel required at destination (future: from AirControlArea) |
| `serviceCeilingM` | `AircraftModel.serviceCeilingM()` | Maximum operating altitude (m) |
| `minAltitudeM` | Destination `Airport.elevationMeters()` | Minimum altitude = destination elevation (m) |
| `segmentAltitudesM[]` | DSL segment altitude slots | Cruise altitudes per segment (m) |

### 3.3 C Physics Validator Logic

#### Fuel Sufficiency Check

```
remaining = fuelOnBoardKg - reserveFuelKg

if remaining < minFuelKg
    FAIL|FUEL_INSUFFICIENT|remaining=%.2f|required=%.2f
else if fuelOnBoardKg <= 0
    FAIL|FUEL_INVALID|fuelOnBoardKg must be positive
```

#### Altitude Check

For each segment altitude in `segmentAltitudesM[]`:

```
if cruise_alt > service_ceiling
    FAIL|ALTITUDE_ABOVE_CEILING|segment=%d|cruise=%.2f|ceiling=%.2f

if cruise_alt < min_alt
    FAIL|ALTITUDE_BELOW_MIN|segment=%d|cruise=%.2f|min=%.2f
```

### 3.4 Build and Invocation

```bash
# Build the C validator
cd lapr.base
make compile

# The executable is produced at:
#   lapr.base/flight_plan_validator
```

Java invokes it as:

```java
ProcessBuilder pb = new ProcessBuilder("./flight_plan_validator", inputJson.toString());
pb.directory(validatorBaseDir.toFile());
pb.redirectErrorStream(true);
```

### 3.5 Error Codes

| Code | Meaning | Exit Code |
|------|---------|-----------|
| `PASS` | All checks passed | 0 |
| `FUEL_INVALID` | fuelOnBoardKg <= 0 | 1 |
| `FUEL_INSUFFICIENT` | remaining < minFuelKg | 1 |
| `ALTITUDE_ABOVE_CEILING` | cruise alt > service ceiling | 1 |
| `ALTITUDE_BELOW_MIN` | cruise alt < destination elevation | 1 |
| `ALTITUDE_DATA_MISSING` | segment altitudes array missing/empty | 1 |
| (input error) | Cannot read JSON file | 2 |

---

## 4. Physics Model Summary

The following simplified physics model is used by the C validator:

### Lift and Drag

```
L = Cl * A * (ρ * v²) / 2
D = Cd * A * (ρ * v²) / 2
```

Where:
- `Cl` = lift coefficient (from AircraftModel)
- `Cd` = drag coefficient (`cd0` from AircraftModel)
- `A` = wing area (m², from AircraftModel)
- `ρ` = air density at altitude: `ρ = 1.225 * (1 - 0.0000226 * h)^4.256` (kg/m³, h in meters)
- `v` = cruise speed (m/s, from AircraftModel)

### Fuel Consumption (simplified Breguet)

```
Fuel_burned = Wi - Wf
Wf = Wi / exp( Range * TSFC / (TAS * (L/D)) )
```

Where:
- `TAS` = cruise speed (m/s)
- `TSFC` = thrust specific fuel consumption (N/N/s, from EngineModel)
- `Range` = distance of the leg (m, calculated via Haversine)
- `Wi` = initial weight for leg (emptyWeight + cargo + fuel)
- `L/D` = Cl / Cd
- Fuel density: JET A-1 = 0.804 kg/l

**Note:** The current C validator implementation (`flight_validator.c`) checks **total fuel remaining >= min fuel** rather than computing per-leg Breguet burn. The Breguet model is specified as the target physics but full per-leg simulation is reserved for future refinement.
