package eapli.aisafe.reporting.domain;

/**
 * Strategy interface for report data collection.
 * Each report type supplies its own implementation that knows how to gather
 * the specific data it needs.
 */
public interface ReportDataCollector<T> {

    T collect();
}
