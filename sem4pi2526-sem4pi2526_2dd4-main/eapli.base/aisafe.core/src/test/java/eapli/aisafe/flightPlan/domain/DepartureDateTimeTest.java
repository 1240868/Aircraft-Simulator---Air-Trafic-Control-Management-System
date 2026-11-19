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
