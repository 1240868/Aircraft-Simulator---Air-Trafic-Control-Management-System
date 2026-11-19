package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportId;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import java.util.Optional;

class JpaAirportRepository
        extends BaseJpaRepositoryBase<Airport, String, String>
        implements AirportRepository {

    JpaAirportRepository() {
        super("icaoCode");
    }

    @Override
    public Optional<Airport> findByIcaoCode(final String icaoCode) {
        return matchOne("e.icaoCode=:icaoCode", "icaoCode", icaoCode);
    }

    @Override
    public Optional<Airport> findByIataCode(final String iataCode) {
        return matchOne("e.iataCode=:iataCode", "iataCode", iataCode);
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
        return match("e.airControlArea.code = :code", "code", areaCode);
    }
}
