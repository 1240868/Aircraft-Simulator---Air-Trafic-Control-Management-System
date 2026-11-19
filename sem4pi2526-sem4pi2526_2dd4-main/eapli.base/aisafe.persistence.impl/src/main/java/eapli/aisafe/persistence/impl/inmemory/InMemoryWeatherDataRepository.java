package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryWeatherDataRepository
    extends InMemoryDomainRepository<WeatherData, WeatherDataId>
    implements WeatherDataRepository {

    @Override
    public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
        final List<WeatherData> result = new ArrayList<>();
        for (final var wd : super.findAll()) {
            if (wd.areaCode().toString().equals(areaCode.toString())) {
                result.add(wd);
            }
        }
        return result;
    }

    @Override
    public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                      final Instant fromInclusive,
                                                      final Instant toExclusive) {
        final List<WeatherData> result = new ArrayList<>();
        for (final var wd : super.findAll()) {
            if (wd.areaCode().toString().equals(areaCode.toString())
                && !wd.observedAt().isBefore(fromInclusive)
                && wd.observedAt().isBefore(toExclusive)) {
                result.add(wd);
            }
        }
        return result;
    }
}

