package eapli.aisafe.weather.application;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.weather.application.consultation.WeatherConsultationResult;
import eapli.aisafe.weather.application.consultation.WeatherDataSummary;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherQueryDate;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only weather consultation by air control area and UTC day (US043). */
public class ConsultWeatherDataService {

    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;

    public ConsultWeatherDataService(final WeatherDataRepository weatherRepo,
                                     final AirControlAreaRepository areaRepo) {
        this.weatherRepo = weatherRepo;
        this.areaRepo = areaRepo;
    }

    public WeatherConsultationResult consult(final String areaCodeRaw, final LocalDate date) {
        final var code = AirControlAreaCode.valueOf(areaCodeRaw);
        if (areaRepo.findByCode(code).isEmpty()) {
            throw new IllegalArgumentException("Unknown air control area: " + code);
        }

        final WeatherQueryDate queryDate = new WeatherQueryDate(date);
        final List<WeatherData> records = new ArrayList<>();
        for (final WeatherData wd : weatherRepo.findByAreaCodeBetween(
                code, queryDate.startOfDayUtc(), queryDate.endOfDayExclusiveUtc())) {
            records.add(wd);
        }
        records.sort(Comparator.comparing(WeatherData::observedAt));

        final List<WeatherDataSummary> summaries = records.stream()
                .map(this::toSummary)
                .toList();

        return new WeatherConsultationResult(code.toString(), date, summaries.size(), summaries);
    }

    private WeatherDataSummary toSummary(final WeatherData wd) {
        final String source = wd.source() != null ? wd.source().value() : "N/A";
        return new WeatherDataSummary(
                wd.identity().toString(),
                wd.areaCode().toString(),
                wd.observedAt(),
                wd.temperatureC(),
                wd.humidityPercent(),
                wd.windSpeedMs(),
                wd.windDirectionDegrees(),
                source);
    }
}
