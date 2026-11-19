# Aggregate Justification: FlightPlan

## Invariant

The `FlightPlan` aggregate enforces that **the arrival airport of leg N must exactly match the departure airport of leg N+1**, ensuring geographical continuity across all legs. A flight plan that violates this rule describes a physically impossible journey and must be rejected.

## Design decisions

### FlightPlanState

Because `FlightPlan` is a reusable template that multiple `Flight` instances can reference (1-N relationship), it carries its own minimal state:

| State      | Meaning |
|------------|---------|
| `BUILDING` | Legs can still be added or removed |
| `LOCKED`   | At least one `Flight` validated against this plan — the plan is now immutable |

This state is independent of each `Flight`'s `FlightValidationStatus` (DRAFT / VALIDATED / VOID / REJECTED), which lives on the `Flight` aggregate. Locking the plan prevents retroactive changes that would silently invalidate already-validated flights.

### Application Service layer

The `AddLegToFlightPlanService` sits between the UI controller and the domain. It is responsible for:
- Loading the aggregate via the repository
- Coordinating VO construction and aggregate method calls
- Persisting the modified aggregate

The aggregate root (`FlightPlan`) never touches the repository directly.

## Sequence — adding a leg

**Step 1 – Entry point**
The Pilot triggers `addLeg(planId, legData)` through the UI controller, which immediately delegates to `AddLegToFlightPlanService`.

**Step 2 – Load aggregate**
The service loads the `FlightPlan` aggregate root from `FlightPlanRepository` by its identity.

**Step 3 – Value Object construction**
A `FlightLeg` value object is constructed from the raw data. The VO validates its own internal invariants at construction time:
- Departure airport ≠ arrival airport.
- Fuel quantity must be strictly positive.
- The leg must contain at least one segment.

If any of these fail, a `DomainException` is thrown immediately and the aggregate is never touched.

**Step 4 – Aggregate enforces business invariants**
`FlightPlan.addLeg(newLeg)` enforces two invariants:

1. **Immutability invariant**: if `state == LOCKED`, the plan is immutable and the operation is rejected with `DomainException("FlightPlan is locked")`.
2. **Sequence continuity invariant**: if the leg collection is non-empty, `arrival(lastLeg).airport` must equal `departure(newLeg).airport`. A mismatch is rejected with `DomainException("Leg sequence mismatch")`.

If both checks pass, the leg is appended to the internal collection.

**Step 5 – Persistence**
The service persists the modified aggregate via `FlightPlanRepository.save(fp)`.

## Sequence Diagram
![Sequence Diagram](docs/global-artifacts/Sequence-Diagrams/FlightPlan-Agg-Scenario/CheckFlightStages.svg)
