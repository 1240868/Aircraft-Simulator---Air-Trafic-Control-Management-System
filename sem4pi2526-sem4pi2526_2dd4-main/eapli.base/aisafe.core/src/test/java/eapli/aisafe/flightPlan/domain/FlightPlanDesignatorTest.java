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
