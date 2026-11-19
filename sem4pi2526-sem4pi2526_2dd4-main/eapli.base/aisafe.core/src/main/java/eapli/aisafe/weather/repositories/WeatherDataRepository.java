package eapli.aisafe.weather.repositories;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.framework.domain.repositories.DomainRepository;
import java.time.Instant;

public interface WeatherDataRepository extends DomainRepository<WeatherDataId, WeatherData> {
    Iterable<WeatherData> findByAreaCode(AirControlAreaCode areaCode);

    Iterable<WeatherData> findByAreaCodeBetween(AirControlAreaCode areaCode, Instant fromInclusive, Instant toExclusive);
}

