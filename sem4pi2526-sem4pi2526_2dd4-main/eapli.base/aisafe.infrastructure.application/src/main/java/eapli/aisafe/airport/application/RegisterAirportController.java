package eapli.aisafe.airport.application;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;

@UseCaseController
public class RegisterAirportController {

    private final AuthorizationPort authz;
    private final AirportRepository airportRepo;
    private final AirControlAreaRepository areaRepo;

    public RegisterAirportController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airControlAreas());
    }

    public RegisterAirportController(final AuthorizationPort authz,
                                     final AirportRepository airportRepo,
                                     final AirControlAreaRepository areaRepo) {
        this.authz = authz;
        this.airportRepo = airportRepo;
        this.areaRepo = areaRepo;
    }

    public Airport registerAirport(final String icaoCode, final String iataCode,
                                   final String name, final String town, final String country,
                                   final double latitude, final double longitude,
                                   final double elevationMeters, final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);

        if (airportRepo.findByIcaoCode(icaoCode).isPresent()) {
            throw new IntegrityViolationException("ICAO code already exists: " + icaoCode);
        }
        if (airportRepo.findByIataCode(iataCode).isPresent()) {
            throw new IntegrityViolationException("IATA code already exists: " + iataCode);
        }

        final AirControlArea area = areaRepo.findByCode(AirControlAreaCode.valueOf(areaCode))
                .orElseThrow(() -> new IllegalArgumentException("Air control area not found: " + areaCode));

        final var airport = new Airport(icaoCode, iataCode, name, town, country,
                latitude, longitude, elevationMeters, area);
        return airportRepo.save(airport);
    }

    public Iterable<AirControlArea> listAirControlAreas() {
        return areaRepo.findAll();
    }
}
