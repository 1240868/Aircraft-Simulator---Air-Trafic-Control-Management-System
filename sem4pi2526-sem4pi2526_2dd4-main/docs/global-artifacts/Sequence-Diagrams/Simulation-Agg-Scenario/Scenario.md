# Aggregate Justification: Simulation

## Invariant

The `Simulation` aggregate enforces that **a simulation can only be finalised once**. Calling `finalise()` on an already-completed simulation is rejected with a `DomainException`. Once finalised, the `SimulationResult` value object is set and the aggregate becomes immutable.

## Design decisions

### One aggregate, not two

`Simulation` holds both the run parameters (area, who requested it, when) and, once completed, its result (`SimulationResult`). A separate `SimulationReport` aggregate was considered but rejected: the result has no independent identity — it cannot be navigated to or queried without knowing the simulation it belongs to. Keeping both inside one aggregate boundary enforces atomicity and avoids unnecessary cross-aggregate coordination.

### SimulationResult as a Value Object

`SimulationResult` is a Value Object embedded inside `Simulation`. It is `null` while `status != COMPLETED` and is created atomically by `finalise()`. Because it is a VO, it is immutable by construction — there is no way to partially update it after creation.

### SimulationStatus lifecycle

| Status      | Meaning |
|-------------|---------|
| `PENDING`   | Simulation requested but not yet started |
| `RUNNING`   | C process is executing; flight results and violations may be recorded |
| `COMPLETED` | `finalise()` was called; `SimulationResult` is set and immutable |
| `FAILED`    | The C process terminated abnormally; no result is produced |

`finalise()` may only be called from `RUNNING`. Any other state causes a `DomainException`.

### Application Service layer

`FinaliseSimulationService` sits between the controller and the domain. It is responsible for:
- Loading the `Simulation` aggregate via `SimulationRepository`
- Resolving the `AirControlArea` reference (by `sim.areaId`) to obtain `minFuelReserve`
- Passing `minFuelReserve` to `sim.finalise()` so the aggregate can compute the outcome
- Persisting the modified aggregate

The aggregate root never touches repositories directly.

### Cross-aggregate reference by identity

`Simulation` stores only `areaId` (not the `AirControlArea` object). The Application Service resolves this reference at runtime via `AirControlAreaRepository`. This follows the DDD principle of cross-aggregate reference by identity, keeping aggregates loosely coupled.

### File output

`SimulationResult` contains a `ReportFileRef` value object that records the filename and format of the output file written after finalisation. Writing the file is an infrastructure concern handled by the Application Service (or a dedicated port/adapter) after `finalise()` returns, not by the aggregate itself.

## Sequence — finalising a simulation

**Step 1 – Entry point**
The Flight Control Operator triggers `finaliseSimulation(simulationId)` through the UI controller, which delegates immediately to `FinaliseSimulationService`.

**Step 2 – Load aggregate**
The service loads the `Simulation` aggregate from `SimulationRepository` by its identity.

**Step 3 – Resolve external reference**
The service loads the target `AirControlArea` (using `sim.areaId`) from `AirControlAreaRepository` and queries it for `minFuelReserve`. This value encapsulates domain knowledge that belongs to the area, not to the simulation.

**Step 4 – Aggregate enforces the finalisation invariant**
`Simulation.finalise(minReserve)` enforces two guards:

1. **Already-finalised guard**: if `status == COMPLETED`, the aggregate rejects the call with `DomainException("Simulation already finalised")`.
2. **Invalid-state guard**: if `status != RUNNING`, the call is rejected with `DomainException("Simulation not in RUNNING state")`.

If both pass, the aggregate:
- Computes `overallOutcome` — `FAIL` if any recorded `SafetyViolation` is of type `FUEL_RESERVE`, `PASS` otherwise.
- Creates the immutable `SimulationResult` VO.
- Transitions `status` to `COMPLETED`.

**Step 5 – Persistence**
The service persists the completed aggregate via `SimulationRepository.save(sim)`.

## Sequence Diagram
![Sequence Diagram](docs/global-artifacts/Sequence-Diagrams/Simulation-Agg-Scenario/Simulation-Agg-Scenario.svg)
