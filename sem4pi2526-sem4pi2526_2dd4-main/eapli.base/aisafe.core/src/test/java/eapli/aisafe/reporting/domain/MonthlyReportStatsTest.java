package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MonthlyReportStatsTest {

    private static MonthlyReportStats buildStats(final int total, final int approved,
                                                  final int rejected, final int draft) {
        return MonthlyReportStats.builder()
                .total(total).approved(approved).rejected(rejected).draft(draft)
                .build();
    }

    @Test
    public void shouldStoreTotalFlightPlans() {
        assertEquals(10, buildStats(10, 6, 3, 1).totalFlightPlans());
    }

    @Test
    public void shouldStoreApprovedCount() {
        assertEquals(6, buildStats(10, 6, 3, 1).approvedFlightPlans());
    }

    @Test
    public void shouldStoreRejectedCount() {
        assertEquals(3, buildStats(10, 6, 3, 1).rejectedFlightPlans());
    }

    @Test
    public void shouldStoreDraftCount() {
        assertEquals(1, buildStats(10, 6, 3, 1).draftFlightPlans());
    }

    @Test
    public void shouldAcceptAllZeros() {
        assertEquals(0, buildStats(0, 0, 0, 0).totalFlightPlans());
    }

    @Test
    public void shouldStoreDistinctPilots() {
        final MonthlyReportStats stats = MonthlyReportStats.builder()
                .total(5).distinctPilots(3).build();
        assertEquals(3, stats.distinctPilots());
    }

    @Test
    public void shouldStoreDistinctAircraft() {
        final MonthlyReportStats stats = MonthlyReportStats.builder()
                .total(5).distinctAircraft(2).build();
        assertEquals(2, stats.distinctAircraft());
    }

    @Test
    public void shouldStoreFlightTypeCounts() {
        final MonthlyReportStats stats = MonthlyReportStats.builder()
                .total(4).commercial(2).cargo(1).charter(1).build();
        assertEquals(2, stats.commercialFlights());
        assertEquals(1, stats.cargoFlights());
        assertEquals(1, stats.charterFlights());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeTotal() {
        MonthlyReportStats.builder().total(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeApproved() {
        MonthlyReportStats.builder().approved(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeRejected() {
        MonthlyReportStats.builder().rejected(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeDraft() {
        MonthlyReportStats.builder().draft(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeDistinctPilots() {
        MonthlyReportStats.builder().distinctPilots(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeDistinctAircraft() {
        MonthlyReportStats.builder().distinctAircraft(-1).build();
    }
}
