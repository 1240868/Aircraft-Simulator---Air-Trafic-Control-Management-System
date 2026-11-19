# Aggregate Justification: WeatherData

## Invariant

The `WeatherData` aggregate enforces that **no two readings may share the same altitude within the same data set**, and that all readings must be internally coherent (e.g. cloud ceiling cannot exceed the declared service ceiling). A reading that violates either rule is rejected before persistence.

## Design decisions

### Application Service layer

`AddWeatherReadingService` sits between the UI controller and the domain. It is responsible for:
- Loading the aggregate via `WeatherDataRepository`
- Coordinating Value Object construction from raw input data
- Calling the aggregate method and persisting the result

The controller never touches the repository or the aggregate directly.

### Value Object construction — two validation tiers

Validation is split into two tiers with distinct responsibilities:

| Tier | Where | What is checked |
|------|-------|-----------------|
| Physical limits | `WeatherReading` constructor | windSpeed ≥ 0, visibility ≥ 0, altitude within valid range — constraints that are always true regardless of context |
| Domain invariants | `WeatherData.addReading()` | duplicate altitude, mutual incoherence between readings — constraints that depend on the aggregate's current state |

If any physical-limit check fails, a `DomainException` is thrown immediately and the aggregate is never touched.

### Aggregate boundary

`WeatherData.addReading()` is the sole guardian of domain-level consistency. No external actor can bypass its checks, and the aggregate is the only place where the "no duplicate altitude" and "coherent readings" invariants are enforced.

## Sequence — adding a reading

**Step 1 – Entry point**
The Weather Person triggers `addReading(dataId, readingData)` through the UI controller, which immediately delegates to `AddWeatherReadingService`.

**Step 2 – Load aggregate**
The service loads the `WeatherData` aggregate root from `WeatherDataRepository` by its identity.

**Step 3 – Value Object construction**
A `WeatherReading` VO is constructed from raw input. Its constructor validates absolute physical limits:
- Wind speed must be ≥ 0.
- Visibility must be ≥ 0.
- Altitude must be within the physically valid range.

If any of these fail, a `DomainException` is thrown and the aggregate is never touched.

**Step 4 – Aggregate enforces domain invariants**
`WeatherData.addReading(reading)` enforces two invariants:

1. **Duplicate altitude**: if `readings` already contains an entry at the same altitude, the call is rejected with `DomainException("Duplicate altitude reading")`.
2. **Mutual incoherence**: if the new reading's values are inconsistent with existing readings (e.g. cloud ceiling above service ceiling), the call is rejected with `DomainException("Incoherent reading values")`.

If both pass, the reading is appended to the internal collection.

**Step 5 – Persistence**
The service persists the modified aggregate via `WeatherDataRepository.save(data)`.

## Sequence Diagram
![Sequence Diagram](docs/global-artifacts/Sequence-Diagrams/WeatherData-Agg-Scenario/CheckWeatherReadings.svg)
