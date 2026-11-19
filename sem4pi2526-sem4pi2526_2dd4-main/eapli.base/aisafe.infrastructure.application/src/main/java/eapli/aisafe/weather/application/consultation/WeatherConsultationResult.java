package eapli.aisafe.weather.application.consultation;

import java.time.LocalDate;
import java.util.List;

/** Result of a read-only weather consultation by area and day (US043). */
public record WeatherConsultationResult(
        String areaCode,
        LocalDate date,
        int recordCount,
        List<WeatherDataSummary> records
) {
}
