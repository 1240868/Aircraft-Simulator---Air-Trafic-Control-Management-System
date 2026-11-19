package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import java.time.YearMonth;
import org.junit.Test;

public class MonthlyStatisticsReportTest {

    private static final AirControlAreaCode AREA = AirControlAreaCode.valueOf("LPPO");
    private static final YearMonth PERIOD = YearMonth.of(2026, 5);
    private static final MonthlyReportStats STATS = MonthlyReportStats.builder()
            .total(5).approved(3).rejected(1).draft(1)
            .commercial(3).cargo(2)
            .distinctPilots(4).distinctAircraft(3)
            .build();

    @Test
    public void titleShouldContainAreaCodeAndPeriod() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        assertTrue(report.title().contains("LPPO"));
        assertTrue(report.title().contains("2026-05"));
    }

    @Test
    public void renderShouldContainAISafeBranding() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        assertTrue(report.render().contains("AISafe"));
    }

    @Test
    public void renderShouldContainStatusSection() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        final String rendered = report.render();
        assertTrue(rendered.contains("Flight Plans by Status"));
        assertTrue(rendered.contains("Approved"));
        assertTrue(rendered.contains("Rejected"));
    }

    @Test
    public void renderShouldContainTypeSection() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        final String rendered = report.render();
        assertTrue(rendered.contains("Flight Plans by Type"));
        assertTrue(rendered.contains("Commercial"));
        assertTrue(rendered.contains("Cargo"));
    }

    @Test
    public void renderShouldContainActivitySection() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        final String rendered = report.render();
        assertTrue(rendered.contains("Monthly Activity"));
        assertTrue(rendered.contains("Distinct pilots"));
        assertTrue(rendered.contains("Distinct aircraft"));
    }

    @Test
    public void renderShouldContainStatValues() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        final String rendered = report.render();
        assertTrue(rendered.contains("5"));
        assertTrue(rendered.contains("3"));
        assertTrue(rendered.contains("4"));
    }

    @Test
    public void generatedAtShouldNotBeNull() {
        final MonthlyStatisticsReport report =
                new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, STATS);
        assertNotNull(report.generatedAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullAreaCode() {
        new MonthlyStatisticsReport(null, "Porto FIR", PERIOD, STATS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullPeriod() {
        new MonthlyStatisticsReport(AREA, "Porto FIR", null, STATS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullStats() {
        new MonthlyStatisticsReport(AREA, "Porto FIR", PERIOD, null);
    }
}
