# US082 – Unit Tests: Insert Weather Data into a Flight Plan

> **User Story:** As a Pilot, I want to add weather data to a flight plan of mine.

---

## Test class

`InsertWeatherDataInFlightControllerTest`

Located at `eapli.base/aisafe.infrastructure.application/src/test/java/eapli/aisafe/flightPlan/application/InsertWeatherDataInFlightControllerTest.java`.

This class tests the `InsertWeatherDataInFlightController` using fake implementations of `AuthorizationPort`, `FlightPlanRepository`, and `WeatherDataRepository`.

---

## Setup (`@Before`)

```java
@Before
public void setUp() {
    auth = new FakeAuth();
    flightPlanRepo = new TestFlightPlanRepo();
    weatherDataRepo = new TestWeatherDataRepo();
    controller = new InsertWeatherDataInFlightController(auth, flightPlanRepo, weatherDataRepo);

    pilotId = PilotId.newId();
    final AirportId dep = AirportId.newId();
    final AirportId arr = AirportId.newId();
    legId = FlightLegId.newId();

    final FlightLeg leg = new FlightLeg(legId, dep, arr, 1,
            List.of(FlightSegmentId.newId()),
            new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM));

    flightPlanId = FlightPlanId.newId();
    flightPlan = new FlightPlan(
            flightPlanId,
            RegistrationNumber.valueOf("CS-TTK"),
            FlightRouteId.newId(),
            new FlightPlanDesignator("FP001"),
            FlightType.COMMERCIAL,
            new DepartureDateTime(LocalDateTime.of(2026, 6, 10, 14, 30)),
            List.of(leg),
            new FlightWind(new WindSpeed(10, "m/s"), new WindDirection(180), MeasurementUnit.KILOGRAM));
    flightPlan.setPilot(pilotId);
    flightPlanRepo.store.put(flightPlanId, flightPlan);
}
```

Shared objects: `pilotId` (owner), `flightPlanId`, `legId`, a `FlightPlan` with status `DRAFT` and one leg.

---

## Test 1 — `shouldInsertWeatherDataSuccessfully`

**Foco:** Happy path — weather data is inserted and persisted correctly.

```java
@Test
public void shouldInsertWeatherDataSuccessfully() {
    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            flightPlanId.toString(),
            legId.toString(),
            "LPPC",
            Instant.now(),
            12.5, 90.0, 20.0, 55.0);

    controller.insertWeatherData(dto, pilotId);

    final FlightPlan saved = flightPlanRepo.store.get(flightPlanId);
    assertNotNull(saved);
    assertNotNull(saved.getLegs().get(0).weatherData());
    assertEquals(12.5, saved.getLegs().get(0).weatherData().windSpeedMs(), 1e-9);
}
```

**Verifica:**
- The WeatherData entity is created and persisted
- The WeatherData is assigned to the correct leg
- The wind speed value is preserved

---

## Test 2 — `shouldThrowWhenFlightPlanNotFound`

**Foco:** Invalid flight plan ID throws IllegalArgumentException.

```java
@Test(expected = IllegalArgumentException.class)
public void shouldThrowWhenFlightPlanNotFound() {
    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            UUID.randomUUID().toString(),
            legId.toString(),
            "LPPC", Instant.now(),
            10, 180, 25, 50);

    controller.insertWeatherData(dto, pilotId);
}
```

**Verifica:**
- A random UUID (non-existent flight plan) triggers `IllegalArgumentException`

---

## Test 3 — `shouldThrowWhenLegNotFound`

**Foco:** Invalid leg ID throws IllegalArgumentException.

```java
@Test(expected = IllegalArgumentException.class)
public void shouldThrowWhenLegNotFound() {
    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            flightPlanId.toString(),
            FlightLegId.newId().toString(),
            "LPPC", Instant.now(),
            10, 180, 25, 50);

    controller.insertWeatherData(dto, pilotId);
}
```

**Verifica:**
- A random FlightLegId (not part of the flight plan) triggers `IllegalArgumentException` from `FlightPlan.insertWeatherData()`

---

## Test 4 — `shouldThrowWhenPilotDoesNotMatch`

**Foco:** Different pilot cannot modify another pilot's flight plan.

```java
@Test(expected = IllegalArgumentException.class)
public void shouldThrowWhenPilotDoesNotMatch() {
    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            flightPlanId.toString(),
            legId.toString(),
            "LPPC", Instant.now(),
            10, 180, 25, 50);

    controller.insertWeatherData(dto, PilotId.newId());
}
```

**Verifica:**
- A different PilotId (not the owner) triggers `IllegalArgumentException`

---

## Test 5 — `shouldThrowWhenFlightPlanIsCancelled`

**Foco:** CANCELLED flight plan cannot receive weather data.

```java
@Test(expected = IllegalStateException.class)
public void shouldThrowWhenFlightPlanIsCancelled() {
    flightPlan.setStatus(FlightPlanStatus.CANCELLED);
    flightPlanRepo.store.put(flightPlanId, flightPlan);

    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            flightPlanId.toString(),
            legId.toString(),
            "LPPC", Instant.now(),
            10, 180, 25, 50);

    controller.insertWeatherData(dto, pilotId);
}
```

**Verifica:**
- Setting the flight plan status to CANCELLED triggers `IllegalStateException` from `FlightPlan.insertWeatherData()`

---

## Test 6 — `shouldRevertToPendingTestWhenTested`

**Foco:** TESTED flight plan reverts to PENDING_TEST after weather data insertion.

```java
@Test
public void shouldRevertToPendingTestWhenTested() {
    flightPlan.setStatus(FlightPlanStatus.TESTED);
    flightPlanRepo.store.put(flightPlanId, flightPlan);

    final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
            flightPlanId.toString(),
            legId.toString(),
            "LPPC", Instant.now(),
            8.0, 45.0, 22.0, 60.0);

    controller.insertWeatherData(dto, pilotId);

    final FlightPlan saved = flightPlanRepo.store.get(flightPlanId);
    assertEquals(FlightPlanStatus.PENDING_TEST, saved.getStatus());
}
```

**Verifica:**
- The core US082 invariant: inserting weather data into a TESTED flight plan reverts it to PENDING_TEST
- Previous test result is deemed void

---

# Fake implementations used by the tests

## FakeAuth

```java
private static final class FakeAuth implements AuthorizationPort {
    Role[] lastRoles;

    @Override
    public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
        this.lastRoles = roles;
    }

    @Override
    public String currentUsername() {
        return "pilot1";
    }
}
```

## TestWeatherDataRepo

In-memory store backed by `HashMap<WeatherDataId, WeatherData>`.

Implements all `WeatherDataRepository` methods.

## TestFlightPlanRepo

In-memory store backed by `HashMap<FlightPlanId, FlightPlan>`.

Implements all `FlightPlanRepository` methods (`findById`, `save`, `findAll`, etc.).

---

# Business rules covered by the tests

| Test | Business Rule |
|------|--------------|
| Test 1 | WeatherData is created, persisted, and assigned to the correct leg |
| Test 2 | FlightPlan must exist |
| Test 3 | Target leg must exist in the FlightPlan |
| Test 4 | Pilot must own the FlightPlan |
| Test 5 | CANCELLED flight plans cannot receive weather data |
| Test 6 | TESTED → PENDING_TEST after weather data insertion |
