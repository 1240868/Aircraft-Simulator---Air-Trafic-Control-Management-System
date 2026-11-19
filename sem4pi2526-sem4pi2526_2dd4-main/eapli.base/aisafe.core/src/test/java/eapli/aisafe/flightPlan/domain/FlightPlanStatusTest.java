package eapli.aisafe.flightPlan.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FlightPlanStatusTest {

    @Test
    public void shouldHaveDraftConstant() {
        assertEquals("DRAFT", FlightPlanStatus.DRAFT.name());
    }

    @Test
    public void shouldHaveAllExpectedValues() {
        assertNotNull(FlightPlanStatus.valueOf("DRAFT"));
        assertNotNull(FlightPlanStatus.valueOf("SUBMITTED"));
        assertNotNull(FlightPlanStatus.valueOf("APPROVED"));
        assertNotNull(FlightPlanStatus.valueOf("REJECTED"));
        assertNotNull(FlightPlanStatus.valueOf("ACTIVE"));
        assertNotNull(FlightPlanStatus.valueOf("COMPLETED"));
        assertNotNull(FlightPlanStatus.valueOf("CANCELLED"));
    }
}
