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
