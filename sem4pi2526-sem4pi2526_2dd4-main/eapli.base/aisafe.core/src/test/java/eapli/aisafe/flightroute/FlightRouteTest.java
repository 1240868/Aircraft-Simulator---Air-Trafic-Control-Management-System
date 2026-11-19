package eapli.aisafe.flightroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;
import org.junit.Test;

/** US073 — flight route aggregate invariants. */
public class FlightRouteTest {

    @Test
    public void createShouldSetActiveStatus() {
        final var route = sampleRoute("TP123", "LPPT", "LPPR");
        assertEquals(RouteActivationStatus.ACTIVE, route.getStatus());
        assertTrue(route.isActive());
    }

    @Test
    public void createShouldRejectSameOriginAndDestination() {
        assertThrows(IllegalArgumentException.class,
                () -> FlightRoute.create(
                        new FlightRouteName("TP1"),
                        CompanyId.newId(),
                        AirportId.forIcaoCode("LPPT"),
                        AirportId.forIcaoCode("LPPT"),
                        new RouteDistance(100000, MeasurementUnit.METER),
                        new RouteDuration(60)));
    }

    @Test
    public void shouldExposeAggregateState() {
        final CompanyId companyId = CompanyId.newId();
        final var route = FlightRoute.create(
                new FlightRouteName("TP456"),
                companyId,
                AirportId.forIcaoCode("LPPT"),
                AirportId.forIcaoCode("LPPR"),
                new RouteDistance(250000, MeasurementUnit.METER),
                new RouteDuration(30));

        assertEquals("TP456", route.getName().value());
        assertEquals(companyId, route.companyId());
        assertEquals("LPPT", route.originAirportId().code());
        assertEquals("LPPR", route.destinationAirportId().code());
        assertEquals(250000, route.getDistance().value(), 0.001);
        assertEquals(30L, route.getDuration().minutes());
        assertEquals(route.getId(), route.identity());
        assertTrue(route.toString().contains("ACTIVE"));
    }

    @Test
    public void equalsShouldDependOnIdentityOnly() {
        final var a = sampleRoute("TP1", "LPPT", "LPPR");
        final var b = sampleRoute("TP2", "LPPT", "LPPR");
        assertEquals(a, a);
        assertTrue(a != b);
    }

    @Test
    public void equalsAndHashCodeDependOnIdentity() {
        final var route = sampleRoute("TP10", "LPPT", "LPPR");
        assertEquals(route.hashCode(), route.hashCode());
        assertFalse(route.equals(null));
        assertFalse(route.equals("not a route"));
        assertNotEquals(route, sampleRoute("TP11", "LPPT", "LPPR"));
    }

    @Test
    public void sameAsShouldRequireDistinctInstanceWithSameId() {
        final var route = sampleRoute("TP12", "LPPT", "LPPR");
        assertFalse(route.sameAs(route));
        assertFalse(route.sameAs("not a route"));
        assertFalse(route.sameAs(sampleRoute("TP13", "LPPT", "LPPR")));
    }

    @Test
    public void compareToOwnIdentityShouldBeZero() {
        final var route = sampleRoute("TP14", "LPPT", "LPPR");
        assertEquals(0, route.compareTo(route.identity()));
    }

    @Test
    public void createShouldRejectNullArguments() {
        final var name = new FlightRouteName("TP15");
        final var company = CompanyId.newId();
        final var origin = AirportId.forIcaoCode("LPPT");
        final var dest = AirportId.forIcaoCode("LPPR");
        final var dist = new RouteDistance(1000, MeasurementUnit.METER);
        final var dur = new RouteDuration(60);
        assertThrows(NullPointerException.class, () -> FlightRoute.create(null, company, origin, dest, dist, dur));
        assertThrows(NullPointerException.class, () -> FlightRoute.create(name, null, origin, dest, dist, dur));
        assertThrows(NullPointerException.class, () -> FlightRoute.create(name, company, null, dest, dist, dur));
        assertThrows(NullPointerException.class, () -> FlightRoute.create(name, company, origin, null, dist, dur));
        assertThrows(NullPointerException.class, () -> FlightRoute.create(name, company, origin, dest, null, dur));
        assertThrows(NullPointerException.class, () -> FlightRoute.create(name, company, origin, dest, dist, null));
    }

    @Test
    public void ormConstructorShouldExist() {
        assertNotNull(new FlightRoute());
    }

    private static FlightRoute sampleRoute(final String name, final String origin, final String dest) {
        return FlightRoute.create(
                new FlightRouteName(name),
                CompanyId.newId(),
                AirportId.forIcaoCode(origin),
                AirportId.forIcaoCode(dest),
                new RouteDistance(300000, MeasurementUnit.METER),
                new RouteDuration(45));
    }
}
