# US080 — Tests

Os testes unitários estão divididos em duas categorias:

1. **Domain tests** — validam os Value Objects e a entidade `FlightPlan`
2. **Application tests** — validam o `RegisterFlightPlanController`

---

## FlightPlanTest.java

```java
package eapli.aisafe.flightPlan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanTest {

    @Test
    void testToString() {
    }

    @Test
    void testEquals() {
    }

    @Test
    void testHashCode() {
    }

    @Test
    void sameAs() {
    }

    @Test
    void identity() {
    }

    @Test
    void getId() {
    }

    @Test
    void setId() {
    }

    @Test
    void getAircraft() {
    }

    @Test
    void setAircraft() {
    }

    @Test
    void getFlightRoute() {
    }

    @Test
    void setFlightRoute() {
    }

    @Test
    void getDesignator() {
    }

    @Test
    void setDesignator() {
    }

    @Test
    void getStatus() {
    }

    @Test
    void setStatus() {
    }

    @Test
    void getFlightType() {
    }

    @Test
    void setFlightType() {
    }

    @Test
    void getDepartureDateTime() {
    }

    @Test
    void setDepartureDateTime() {
    }

    @Test
    void getLegs() {
    }

    @Test
    void setLegs() {
    }

    @Test
    void getWind() {
    }

    @Test
    void setWind() {
    }

    @Test
    void updateWeatherData() {
    }
}
```

---

## FlightPlanDesignatorTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FlightPlanDesignatorTest {

    @Test
    public void shouldCreateFromValidString() {
        assertEquals("FP001", new FlightPlanDesignator("FP001").value());
    }

    @Test
    public void shouldTrimWhitespace() {
        assertEquals("FP001", new FlightPlanDesignator("  FP001  ").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNull() {
        new FlightPlanDesignator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmpty() {
        new FlightPlanDesignator("");
    }
}
```

---

## FlightLegTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

import org.junit.Test;

import java.util.List;

public class FlightLegTest {

    private static FuelPlan fuel() {
        return new FuelPlan(1000, 200, MeasurementUnit.KILOGRAM);
    }

    @Test
    public void shouldCreateValidLeg() {
        final AirportId dep = AirportId.newId();
        final AirportId arr = AirportId.newId();
        final FlightLeg leg = new FlightLeg(FlightLegId.newId(), dep, arr, 1,
                List.of(FlightSegmentId.newId()), fuel());
        assertEquals(dep, leg.departureAirport());
        assertEquals(arr, leg.arrivalAirport());
        assertEquals(1, leg.legOrder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullDeparture() {
        new FlightLeg(FlightLegId.newId(), null, AirportId.newId(), 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullArrival() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), null, 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroLegOrder() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 0,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeLegOrder() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), -1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptySegments() {
        new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 1,
                List.of(), fuel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSameAirport() {
        final AirportId same = AirportId.newId();
        new FlightLeg(FlightLegId.newId(), same, same, 1,
                List.of(FlightSegmentId.newId()), fuel());
    }

    @Test
    public void shouldStoreFuelPlan() {
        final FuelPlan fp = new FuelPlan(5000, 1000, MeasurementUnit.KILOGRAM);
        final FlightLeg leg = new FlightLeg(FlightLegId.newId(), AirportId.newId(), AirportId.newId(), 1,
                List.of(FlightSegmentId.newId()), fp);
        assertEquals(fp, leg.fuelPlan());
    }
}
```

---

## FlightLegIdTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.*;
import java.util.UUID;
import org.junit.Test;

public class FlightLegIdTest {

    @Test
    public void newIdShouldReturnNonNull() {
        assertNotNull(FlightLegId.newId());
    }

    @Test
    public void equalsSameValue() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(new FlightLegId(uuid), new FlightLegId(uuid));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(FlightLegId.newId(), FlightLegId.newId());
    }
}
```

---

## FlightSegmentTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.*;

import eapli.aisafe.sharedkernel.valueobjects.Altitude;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;

import org.junit.Test;
import org.springframework.data.geo.Distance;

import java.time.Duration;

public class FlightSegmentTest {

    @Test
    public void shouldCreateValidSegment() {
        final FlightSegmentId id = FlightSegmentId.newId();
        final Distance dist = new Distance(100.0);
        final Duration dur = Duration.ofMinutes(30);
        final Altitude alt = new Altitude(10000.0, MeasurementUnit.METER);
        final FlightSegment seg = new FlightSegment(id, dist, dur, alt);
        assertEquals(id, seg.identity());
        assertEquals(dist, seg.distance());
        assertEquals(dur, seg.duration());
        assertEquals(alt, seg.altitude());
    }

    @Test
    public void sameAsShouldMatchByIdentity() {
        final FlightSegmentId id = FlightSegmentId.newId();
        final FlightSegment seg1 = new FlightSegment(id, null, null, null);
        final FlightSegment seg2 = new FlightSegment(id, null, null, null);
        assertTrue(seg1.sameAs(seg2));
    }

    @Test
    public void sameAsShouldRejectDifferentId() {
        final FlightSegment seg1 = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        final FlightSegment seg2 = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        assertFalse(seg1.sameAs(seg2));
    }

    @Test
    public void updateWeatherDataShouldSetWind() {
        final FlightSegment seg = new FlightSegment(FlightSegmentId.newId(), null, null, null);
        final FlightWind wind = new FlightWind(
                new WindSpeed(10.0, "KNOT"),
                new WindDirection(180.0),
                MeasurementUnit.KILOGRAM);
        seg.updateWeatherData(wind);
        assertNotNull(seg.wind());
    }
}
```

---

## FlightSegmentIdTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.*;
import java.util.UUID;
import org.junit.Test;

public class FlightSegmentIdTest {

    @Test
    public void newIdShouldReturnNonNull() {
        assertNotNull(FlightSegmentId.newId());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUUID() {
        new FlightSegmentId(null);
    }

    @Test
    public void equalsSameValue() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(new FlightSegmentId(uuid), new FlightSegmentId(uuid));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(FlightSegmentId.newId(), FlightSegmentId.newId());
    }
}
```

---

## FuelPlanTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

import org.junit.Test;

public class FuelPlanTest {

    @Test
    public void shouldCreateValidFuelPlan() {
        final FuelPlan fp = new FuelPlan(5000.0, 500.0, MeasurementUnit.KILOGRAM);
        assertEquals(5000.0, fp.totalFuel(), 0.0);
        assertEquals(500.0, fp.reserveFuel(), 0.0);
        assertEquals(MeasurementUnit.KILOGRAM, fp.unit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveTotal() {
        new FuelPlan(0.0, 0.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeTotal() {
        new FuelPlan(-100.0, 0.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeReserve() {
        new FuelPlan(5000.0, -1.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReserveExceedingTotal() {
        new FuelPlan(100.0, 200.0, MeasurementUnit.KILOGRAM);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonKilogramUnit() {
        new FuelPlan(5000.0, 500.0, MeasurementUnit.LITER);
    }
}
```

---

## DepartureDateTimeTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.junit.Test;

public class DepartureDateTimeTest {

    @Test
    public void shouldCreateFromValidDateTime() {
        final LocalDateTime dt = LocalDateTime.of(2026, 6, 10, 14, 30);
        assertEquals(dt, new DepartureDateTime(dt).value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNull() {
        new DepartureDateTime(null);
    }
}
```

---

## FlightPlanIdTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.*;
import java.util.UUID;
import org.junit.Test;

public class FlightPlanIdTest {

    @Test
    public void newIdShouldReturnNonNull() {
        assertNotNull(FlightPlanId.newId());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUUID() {
        new FlightPlanId(null);
    }

    @Test
    public void equalsSameValue() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(new FlightPlanId(uuid), new FlightPlanId(uuid));
    }

    @Test
    public void equalsDifferentValue() {
        assertNotEquals(FlightPlanId.newId(), FlightPlanId.newId());
    }

    @Test
    public void compareToEqual() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(0, new FlightPlanId(uuid).compareTo(new FlightPlanId(uuid)));
    }
}
```

---

## FlightTypeTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FlightTypeTest {

    @Test
    public void shouldHaveAllExpectedValues() {
        assertNotNull(FlightType.valueOf("COMMERCIAL"));
        assertNotNull(FlightType.valueOf("CARGO"));
        assertNotNull(FlightType.valueOf("CHARTER"));
        assertNotNull(FlightType.valueOf("PRIVATE"));
        assertNotNull(FlightType.valueOf("MILITARY"));
        assertNotNull(FlightType.valueOf("EMERGENCY"));
    }
}
```

---

## FlightWindTest.java

```java
package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import eapli.aisafe.sharedkernel.valueobjects.WindDirection;
import eapli.aisafe.sharedkernel.valueobjects.WindSpeed;

import org.junit.Test;

public class FlightWindTest {

    @Test
    public void shouldCreateValidFlightWind() {
        final WindSpeed speed = new WindSpeed(25.0, "KNOT");
        final WindDirection dir = new WindDirection(180.0);
        final FlightWind wind = new FlightWind(speed, dir, MeasurementUnit.KILOGRAM);
        assertNotNull(wind);
        assertEquals(MeasurementUnit.KILOGRAM, wind.unit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeWindSpeed() {
        new WindSpeed(-1.0, "KNOT");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyWindSpeedUnit() {
        new WindSpeed(25.0, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidWindDirection() {
        new WindDirection(400.0);
    }
}
```

---

## AltitudeSlotTest.java

```java
package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AltitudeSlotTest {

    @Test
    void shouldCreateValidSlot() {
        final AltitudeSlot slot = new AltitudeSlot(10000.0, 40000.0, MeasurementUnit.METER);
        assertEquals(10000.0, slot.minAltitude());
        assertEquals(40000.0, slot.maxAltitude());
        assertEquals(MeasurementUnit.METER, slot.unit());
    }

    @Test
    void shouldAcceptEqualMinMax() {
        final AltitudeSlot slot = new AltitudeSlot(35000.0, 35000.0, MeasurementUnit.METER);
        assertEquals(35000.0, slot.minAltitude());
        assertEquals(35000.0, slot.maxAltitude());
    }

    @Test
    void shouldRejectMinExceedingMax() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(40000.0, 10000.0, MeasurementUnit.METER));
    }

    @Test
    void shouldRejectNegativeMin() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(-1.0, 10000.0, MeasurementUnit.METER));
    }

    @Test
    void shouldRejectNullUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(10000.0, 40000.0, null));
    }

    @Test
    void shouldRejectNonMeterUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(10000.0, 40000.0, MeasurementUnit.KILOMETER));
    }
}
```

---

## RegisterFlightPlanControllerTest.java

```java
package eapli.aisafe.flightPlan.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.company.domain.*;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.*;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.flightPlan.domain.*;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.*;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.*;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.sharedkernel.valueobjects.*;
import eapli.framework.infrastructure.authz.domain.model.Role;

import org.junit.Before;
import org.junit.Test;

public class RegisterFlightPlanControllerTest {

    private FakeAuth auth;
    private TestFlightRouteRepo routeRepo;
    private TestAircraftRepo aircraftRepo;
    private TestPilotRepo pilotRepo;
    private TestFlightPlanRepo flightPlanRepo;
    private TestAirTransportCompanyRepo companyRepo;
    private TestAircraftModelRepo aircraftModelRepo;
    private RegisterFlightPlanController controller;

    private FlightRoute activeRoute;
    private FlightRouteId routeId;
    private Aircraft aircraft;
    private RegistrationNumber aircraftReg;
    private Pilot pilot;
    private PilotId pilotId;
    private CompanyId companyId;

    @Before
    public void setUp() {
        auth = new FakeAuth();
        routeRepo = new TestFlightRouteRepo();
        aircraftRepo = new TestAircraftRepo();
        pilotRepo = new TestPilotRepo();
        flightPlanRepo = new TestFlightPlanRepo();
        companyRepo = new TestAirTransportCompanyRepo();
        aircraftModelRepo = new TestAircraftModelRepo();
        controller = new RegisterFlightPlanController(auth, flightPlanRepo, routeRepo,
                aircraftRepo, pilotRepo, companyRepo, aircraftModelRepo);

        companyId = new CompanyId(UUID.randomUUID());
        final var modelId = new AircraftModelId(UUID.randomUUID());

        final var company = new AirTransportCompany(
                new CompanyName("Test Airlines"),
                new CompanyIataCode("FP"),
                new CompanyIcaoCode("FPL"),
                new AirOperatorCertificate("AOC-TEST", LocalDate.now().plusYears(1)));
        companyRepo.store.put(companyId, company);

        final var engineModelId = new EngineModelId(UUID.randomUUID());
        final var aircraftModel = new AircraftModel("TestModel", "TestMfr",
                AircraftType.PASSENGER, EngineType.TURBOFAN, 2, 150, 5000.0, List.of(engineModelId));
        aircraftModelRepo.store.put(aircraftModel.identity(), aircraftModel);

        aircraft = AircraftFactory.registerNew("CS-TTK", "PT", modelId, companyId,
                List.of(new CabinSeatBlock("Economy", 150)),
                200, LocalDate.of(2012, 3, 3), LocalDate.of(2012, 6, 1));
        aircraftReg = aircraft.identity();
        aircraftRepo.store.put(aircraftReg, aircraft);

        activeRoute = FlightRoute.create(
                new FlightRouteName("RT001"), companyId,
                AirportId.newId(), AirportId.newId(),
                new RouteDistance(100, MeasurementUnit.METER),
                new RouteDuration(60));
        routeId = activeRoute.identity();
        routeRepo.store.put(routeId, activeRoute);

        pilotId = PilotId.newId();
        pilot = new Pilot(pilotId, new FullName("John", "Doe"),
                new Email("john@example.com"), companyId,
                new PilotCertification("ATP-FAA"),
                new License("LIC-001", LocalDateTime.now().plusYears(1)),
                List.of(aircraftModel.identity()));
        pilotRepo.store.put(pilotId, pilot);
    }

    @Test
    public void shouldRegisterFlightPlanWithValidData() {
        final FlightPlan result = controller.register(
                routeId, aircraftReg, pilotId, "FP001",
                "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
        assertNotNull(result);
        assertNotNull(result.identity());
        assertEquals(pilotId, result.getPilot());
        assertTrue(auth.lastRoles.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenRouteNotFound() {
        controller.register(FlightRouteId.newId(), aircraftReg, pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenRouteNotActive() throws Exception {
        final Field statusField = FlightRoute.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(activeRoute, RouteActivationStatus.INACTIVE);
        controller.register(routeId, aircraftReg, pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAircraftNotFound() {
        controller.register(routeId, RegistrationNumber.valueOf("XX-XXXX"), pilotId,
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotNotFound() {
        controller.register(routeId, aircraftReg, PilotId.newId(),
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotInactive() throws Exception {
        final Pilot inactive = new Pilot(PilotId.newId(), new FullName("Jane", "Doe"),
                new Email("jane@example.com"), companyId,
                new PilotCertification("ATP-FAA"),
                new License("LIC-EXPIRED", LocalDateTime.now().plusYears(1)),
                List.of(new AircraftModelId(UUID.randomUUID())));
        final Field statusField = Pilot.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(inactive, PilotStatus.INACTIVE);
        pilotRepo.store.put(inactive.identity(), inactive);
        controller.register(routeId, aircraftReg, inactive.identity(),
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPilotCompanyDoesNotMatchRouteCompany() {
        final Pilot otherPilot = new Pilot(PilotId.newId(), new FullName("Other", "Pilot"),
                new Email("other@example.com"), new CompanyId(UUID.randomUUID()),
                new PilotCertification("ATP-FAA"),
                new License("LIC-OTHER", LocalDateTime.now().plusYears(1)),
                List.of(new AircraftModelId(UUID.randomUUID())));
        pilotRepo.store.put(otherPilot.identity(), otherPilot);
        controller.register(routeId, aircraftReg, otherPilot.identity(),
                "FP001", "COMMERCIAL", LocalDateTime.of(2026, 6, 10, 14, 30));
    }

    @Test
    public void listFlightRoutesShouldReturnAll() {
        final Iterable<FlightRoute> routes = controller.listFlightRoutes();
        final List<FlightRoute> list = new ArrayList<>();
        routes.forEach(list::add);
        assertEquals(1, list.size());
        assertEquals(routeId, list.get(0).identity());
    }

    @Test
    public void listAircraftShouldReturnAll() {
        final Iterable<Aircraft> list = controller.listAircraft();
        final List<Aircraft> items = new ArrayList<>();
        list.forEach(items::add);
        assertEquals(1, items.size());
        assertEquals(aircraftReg, items.get(0).identity());
    }

    @Test
    public void listPilotsShouldReturnAll() {
        final Iterable<Pilot> list = controller.listPilots();
        final List<Pilot> items = new ArrayList<>();
        list.forEach(items::add);
        assertEquals(1, items.size());
        assertEquals(pilotId, items.get(0).identity());
    }

    // --- Fake / in-memory repository implementations ---
    // (see full source in RegisterFlightPlanControllerTest.java)
}
```

---

## Validações

| Categoria    | Validação                                           | Onde                                              |
|-------------|-----------------------------------------------------|---------------------------------------------------|
| Controller  | Utilizador autenticado tem role `PILOT` ou `ADMIN`  | `register()`, `list*()` — `AuthorizationPort`    |
| Controller  | Rota existe                                         | `flightRouteRepo.findById(routeId.toString())`   |
| Controller  | Rota está `ACTIVE`                                  | `route.getStatus() == ACTIVE`                    |
| Controller  | Aeronave existe                                     | `aircraftRepo.ofIdentity(aircraftReg)`           |
| Controller  | Piloto existe                                       | `pilotRepo.ofIdentity(pilotId)`                  |
| Controller  | Piloto está `ACTIVE`                                | `pilot.getStatus() == ACTIVE`                    |
| Controller  | Company do piloto == company da rota                | `route.companyId().equals(pilot.getCompany())`   |
| Controller  | Aeronave está `ACTIVE`                              | `aircraft.operationalStatus() == ACTIVE`        |
| UI          | Input numérico dentro dos limites da lista (pilotos)| `RegisterFlightPlanUI.doShow()`                  |
| UI          | Input numérico dentro dos limites da lista (rotas)  | `RegisterFlightPlanUI.doShow()`                  |
| UI          | Input numérico dentro dos limites da lista (aeronaves)| `RegisterFlightPlanUI.doShow()`                 |
| UI          | Formato da data/hora `yyyy-MM-dd HH:mm`             | `DateTimeFormatter`                              |
| Domain      | `FlightPlanDesignator` não nulo/vazio               | Construtor — `Preconditions.nonEmpty()`          |
| Domain      | `DepartureDateTime` não nulo                        | Construtor — `Preconditions.nonNull()`           |
| Domain      | `FlightType` pertence ao enum                       | `FlightType.valueOf()`                           |
| Domain      | `FlightPlan` status inicial = `DRAFT`               | Construtor                                       |
| Domain      | `FlightLeg` departure != arrival                    | Construtor                                       |
| Domain      | `FlightLeg` legOrder > 0                            | Construtor                                       |
| Domain      | `FlightLeg` segments não vazia                      | Construtor                                       |
| Domain      | `FuelPlan` totalFuel > 0                            | Construtor                                       |
| Domain      | `FuelPlan` reserveFuel >= 0                         | Construtor                                       |
| Domain      | `FuelPlan` reserveFuel <= totalFuel                 | Construtor                                       |
| Domain      | `FuelPlan` unit == KILOGRAM                         | Construtor                                       |
| Domain      | `FlightWind` direction 0-360                        | `WindDirection`                                   |
| Domain      | `FlightWind` speed >= 0                             | `WindSpeed`                                       |
