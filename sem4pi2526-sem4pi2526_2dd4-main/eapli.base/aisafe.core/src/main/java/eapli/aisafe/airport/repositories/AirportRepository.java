package eapli.aisafe.airport.repositories;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface AirportRepository extends DomainRepository<String, Airport> {
    Optional<Airport> findByIcaoCode(String icaoCode);
    Optional<Airport> findByIataCode(String iataCode);
    Optional<Airport> findById(String code);
    Boolean exists(AirportId id);
    Iterable<Airport> findByAirControlAreaCode(AirControlAreaCode areaCode);
}
