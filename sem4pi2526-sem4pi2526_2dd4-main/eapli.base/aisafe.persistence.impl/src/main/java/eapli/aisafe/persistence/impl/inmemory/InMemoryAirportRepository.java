package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryAirportRepository
        extends InMemoryDomainRepository<Airport, String>
        implements AirportRepository {

    @Override
    public Optional<Airport> findByIcaoCode(final String icaoCode) {
        return matchOne(e -> e.icaoCode().equalsIgnoreCase(icaoCode));
    }

    @Override
    public Optional<Airport> findByIataCode(final String iataCode) {
        return matchOne(e -> e.iataCode().equalsIgnoreCase(iataCode));
    }

    @Override
    public Optional<Airport> findById(final String code) {
        return ofIdentity(code);
    }

    @Override
    public Boolean exists(final AirportId id) {
        return ofIdentity(id.toString()).isPresent();
    }

    @Override
    public Iterable<Airport> findByAirControlAreaCode(final AirControlAreaCode areaCode) {
        return match(e -> e.airControlArea() != null
                && e.airControlArea().identity().equals(areaCode));
    }
}
