package eapli.aisafe.weather.domain;

import eapli.framework.domain.model.ValueObject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

/** UTC calendar-day window for weather data consultation (US043). */
public final class WeatherQueryDate implements ValueObject {

    private final LocalDate date;

    public WeatherQueryDate(final LocalDate date) {
        this.date = Objects.requireNonNull(date, "Date is mandatory");
    }

    public Instant startOfDayUtc() {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Exclusive upper bound (start of next UTC day). */
    public Instant endOfDayExclusiveUtc() {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public LocalDate value() {
        return date;
    }
}
