package eapli.aisafe.reporting.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

public final class ReportSection implements ValueObject {

    private final String title;
    private final String content;

    public ReportSection(final String title, final String content) {
        Preconditions.nonEmpty(title, "Section title is required");
        Preconditions.nonNull(content, "Section content is required");
        this.title = title;
        this.content = content;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }
}
