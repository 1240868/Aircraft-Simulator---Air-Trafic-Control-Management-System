package eapli.aisafe.reporting.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Template Method base class for all AISafe reports.
 * Subclasses define their title and sections; this class owns the rendering
 * skeleton (branding, header, footer) that guarantees consistent structure.
 */
public abstract class Report {

    private static final int WIDTH = 65;
    private static final String BORDER = "=".repeat(WIDTH);
    private static final String DIVIDER = "-".repeat(WIDTH);

    private final LocalDateTime generatedAt;

    protected Report() {
        this.generatedAt = LocalDateTime.now();
    }

    public abstract String title();

    protected abstract List<ReportSection> sections();

    public LocalDateTime generatedAt() {
        return generatedAt;
    }

    public final String render() {
        final StringBuilder sb = new StringBuilder();
        sb.append(BORDER).append("\n");
        sb.append("  AISafe — ").append(title()).append("\n");
        sb.append("  Generated at: ").append(generatedAt).append("\n");
        sb.append(BORDER).append("\n\n");
        for (final ReportSection section : sections()) {
            sb.append("[ ").append(section.title()).append(" ]\n");
            sb.append(DIVIDER).append("\n");
            sb.append(section.content()).append("\n");
        }
        sb.append(BORDER).append("\n");
        return sb.toString();
    }
}
