package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class JpaWeatherDataRepository
        extends BaseJpaRepositoryBase<WeatherData, WeatherDataId, WeatherDataId>
        implements WeatherDataRepository {

    JpaWeatherDataRepository() {
        super("id");
    }

    @Override
    public Iterable<WeatherData> findByAreaCode(final AirControlAreaCode areaCode) {
        return match("e.areaCode = :areaCode", "areaCode", areaCode);
    }

    @Override
    public Iterable<WeatherData> findByAreaCodeBetween(final AirControlAreaCode areaCode,
                                                         final Instant fromInclusive,
                                                         final Instant toExclusive) {
        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode);
        params.put("fromInclusive", fromInclusive);
        params.put("toExclusive", toExclusive);
        return match(
                "e.areaCode = :areaCode AND e.observedAt >= :fromInclusive AND e.observedAt < :toExclusive",
                params);
    }
}
