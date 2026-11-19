# US085 — Test/Validate Flight Plan

## 1. Context

As a **Pilot**, I want to test and validate a flight plan I have created. The validation process combines two independent checks:

1. **DSL validation** (lexical, syntactic, semantic) — reuses the LPROG DSL parser to verify the flight plan's DSL source file is well-formed and complies with business rules.
2. **Physical flight testing** — a C program performs fuel sufficiency and altitude safety calculations using a simplified physics model.

If the flight plan passes both stages, its status transitions to `TESTED`. If either stage fails, the status becomes `REJECTED` and detailed error messages are returned to the user.

---

## 2. Requirements

| ID | Description |
|----|-------------|
| **US085** | As a **Pilot**, I want to test/validate a flight plan I've made. |

### Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | Validation includes both DSL validation (lexical, syntactic, semantic) AND physical flight testing (fuel + altitude). |
| AC2 | The physical test component is implemented in the C language. |
| AC3 | Fuel sufficiency is verified: the aircraft must carry enough fuel to complete each leg AND have at least the minimum fuel quantity required by the destination airport's AirControlArea upon landing. |
| AC4 | Minimum altitude requirements are verified: the aircraft's altitude in each segment must respect the allowed altitude slots defined in the segment. |
| AC5 | The Pilot can only test flight plans that belong to their company. |
| AC6 | The FlightPlan must be in DRAFT, PENDING_TEST, or REJECTED status to be tested. |
| AC7 | Results (pass/fail, detailed errors) are shown to the user and the FlightPlan status is updated accordingly. |

---

## 3. Dependencies

| US | Relationship |
|----|-------------|
| **US030** | Authentication and authorization — Pilot must be authenticated with the `PILOT` role. |
| **US080** | Create a flight plan — a FlightPlan must exist before it can be tested. |
| **US083** | Import flight plan from DSL file — the flight plan must have an associated DSL source file. |
| **US120** | DSL validation — lexical, syntactic, and semantic analysis is reused from the LPROG module. |
| **US070/073** | Aircraft and Route management — aircraft and route data are required for physics calculations. |
| **US050/052** | Airport and AirControlArea registration — destination area minimum fuel and elevation data are used by the C validator. |
| **US055/056** | Aircraft model and engine model registration — aerodynamic coefficients, weight, fuel capacity, and engine TSFC are used by the C validator. |

---

## 4. Solution Approach

### Architecture Overview

The test/validate use case follows a two-stage pipeline:

```
TestFlightPlanUI
    → TestFlightPlanController
        → FlightPlanDslValidationPort.validate(flightPlan)     [Stage 1: DSL]
        → FlightPlanTestPayloadWriter.writePayload(flightPlan)  [prepares JSON]
        → FlightPlanPhysicsTestPort.run(payload)                [Stage 2: C via ProcessBuilder]
    → FlightPlanTestResult (passed + errors)
```

### Stage 1 — DSL Validation

The controller calls `FlightPlanDslFileValidator.validate(flightPlan)`, which:

1. Resolves the DSL source file path from `flightPlan.getDslSourcePath()`.
2. Re-imports the file using the LPROG `FlightPlanImporter`.
3. Collects syntax errors (ANTLR4 `SyntaxError`) and semantic errors (`SemanticError`).
4. Returns a `List<String>` of error messages (empty if valid).

### Stage 2 — Physical Flight Testing (C)

The controller:

1. Calls `FlightPlanTestPayloadBuilder.writePayload(flightPlan)` to produce a JSON file containing:
   - `fuelOnBoardKg` — total fuel across all legs
   - `reserveFuelKg` — reserve fuel from the last leg's FuelPlan
   - `minFuelKg` — minimum fuel required (reserve fuel, until AirControlArea min fuel is modelled)
   - `serviceCeilingM` — from AircraftModel
   - `minAltitudeM` — destination airport elevation
   - `segmentAltitudesM[]` — cruise altitudes per segment (read from DSL via `DslSegmentProfileReader`)
2. Calls `FlightPlanPhysicsValidator.run(payload)`, which:
   - Launches the pre-compiled C executable `flight_plan_validator` via `ProcessBuilder`.
   - The C program reads the JSON, performs fuel and altitude checks.
   - Outputs `PASS` on success or `FAIL|code|message` lines on failure.
3. Java reads the C stdout, collecting error lines.

### Status Update

- Both stages pass → `flightPlan.markTestPassed()` → status = `TESTED`
- Either stage fails → `flightPlan.markTestFailed()` → status = `REJECTED`
- Status is persisted via `FlightPlanRepository.save()`

---

## 5. How to Run / Test

### Prerequisites

1. Compile the C validator: `cd lapr.base && make compile`
2. Ensure a flight plan with a valid DSL source path exists (created via US080/US083).

### Running via UI

1. Log in as a Pilot (`pilot1` / `Pilot123!`).
2. Navigate to "Test / Validate Flight Plan" in the main menu.
3. Select a testable flight plan from the list (only DRAFT, PENDING_TEST, or REJECTED plans owned by you are shown).
4. The system runs DSL validation first, then the C physics validator.
5. Results (pass/fail with detailed errors) are displayed.

### Running Tests

```bash
cd lapr.base/aisafe.flightplan.validation
mvn test
```

Tests use `RecordingDslValidator` and `RecordingPhysicsTester` to simulate the external components without requiring the C executable or actual DSL files.

---

## 6. Key Files

| Layer | File | Package |
|-------|------|---------|
| **UI** | `TestFlightPlanUI.java` | `eapli.aisafe.app.user.console.presentation.flightPlan` |
| **Controller** | `TestFlightPlanController.java` | `eapli.aisafe.flightPlan.application` |
| **DSL Validation Port** | `FlightPlanDslValidationPort.java` | `eapli.aisafe.flightPlan.application` |
| **DSL Validator** | `FlightPlanDslFileValidator.java` | `eapli.aisafe.flightPlan.application` |
| **Physics Test Port** | `FlightPlanPhysicsTestPort.java` | `eapli.aisafe.flightPlan.application` |
| **Physics Validator** | `FlightPlanPhysicsValidator.java` | `eapli.aisafe.flightPlan.application` |
| **Payload Builder** | `FlightPlanTestPayloadBuilder.java` | `eapli.aisafe.flightPlan.application` |
| **Payload Writer IF** | `FlightPlanTestPayloadWriter.java` | `eapli.aisafe.flightPlan.application` |
| **DSL Profile Reader** | `DslSegmentProfileReader.java` | `eapli.aisafe.flightPlan.application` |
| **Result** | `FlightPlanTestResult.java` | `eapli.aisafe.flightPlan.application` |
| **Domain** | `FlightPlan.java` (+ `markTestPassed()`, `markTestFailed()`) | `eapli.aisafe.flightPlan.domain` |
| **C Source** | `flight_validator.c`, `flight_validator_main.c` | `lapr.base/src/` |
| **C Header** | `flight_validator.h` | `lapr.base/include/` |
| **Tests** | `TestFlightPlanControllerTest.java` | (test) `eapli.aisafe.flightPlan.application` |
