package eapli.aisafe.flightroute.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.domain.AirOperatorCertificate;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIataCode;
import eapli.aisafe.company.domain.CompanyIcaoCode;
import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.domain.CompanyName;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.IntegrityViolationException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** US073 — flight route creation service. */
public class CreateFlightRouteServiceTest {

    @Mock
    private FlightRouteRepository routeRepo;
    @Mock
    private AirportRepository airportRepo;
    @Mock
    private AirTransportCompanyRepository companyRepo;

    private CreateFlightRouteService service;
    private CompanyId companyId;
    private AirTransportCompany company;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CreateFlightRouteService(routeRepo, airportRepo, companyRepo);
        company = new AirTransportCompany(
                new CompanyName("Test Air"),
                new CompanyIataCode("TP"),
                new CompanyIcaoCode("TST"),
                new AirOperatorCertificate("CERT-1", LocalDate.now().plusYears(1)));
        companyId = company.identity();
        company.addCollaborator("atcc");
    }

    @Test
    public void createRouteShouldPersistWhenAirportsOperational() {
        when(companyRepo.findCompanyIdForCollaboratorUsername("atcc")).thenReturn(Optional.of(companyId));
        when(companyRepo.ofIdentity(companyId)).thenReturn(Optional.of(company));
        when(routeRepo.existsByName(any())).thenReturn(false);
        when(airportRepo.findByIcaoCode("LPPT")).thenReturn(Optional.of(airport("LPPT", "LIS", 38.77, -9.13)));
        when(airportRepo.findByIcaoCode("LPPR")).thenReturn(Optional.of(airport("LPPR", "OPO", 41.25, -8.68)));
        when(routeRepo.save(any(FlightRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        final FlightRouteDTO dto = service.createRoute("77", "LPPT", "LPPR", "atcc");

        assertEquals("TP77", dto.routeName());
        verify(routeRepo).save(any(FlightRoute.class));
    }

    @Test
    public void createRouteShouldRejectUnknownAirport() {
        when(companyRepo.findCompanyIdForCollaboratorUsername("atcc")).thenReturn(Optional.of(companyId));
        when(companyRepo.ofIdentity(companyId)).thenReturn(Optional.of(company));
        when(routeRepo.existsByName(any())).thenReturn(false);
        when(airportRepo.findByIcaoCode("LPPT")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.createRoute("1", "LPPT", "LPPR", "atcc"));
    }

    @Test
    public void createRouteShouldRejectNonOperationalAirport() {
        when(companyRepo.findCompanyIdForCollaboratorUsername("atcc")).thenReturn(Optional.of(companyId));
        when(companyRepo.ofIdentity(companyId)).thenReturn(Optional.of(company));
        when(routeRepo.existsByName(any())).thenReturn(false);

        final Airport closed = mock(Airport.class);
        when(closed.isOperational()).thenReturn(false);
        when(airportRepo.findByIcaoCode("LPPT")).thenReturn(Optional.of(closed));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRoute("1", "LPPT", "LPPR", "atcc"));
    }

    @Test
    public void createRouteShouldRejectDuplicateName() {
        when(companyRepo.findCompanyIdForCollaboratorUsername("atcc")).thenReturn(Optional.of(companyId));
        when(companyRepo.ofIdentity(companyId)).thenReturn(Optional.of(company));
        when(routeRepo.existsByName(any())).thenReturn(true);

        assertThrows(IntegrityViolationException.class,
                () -> service.createRoute("999", "LPPT", "LPPR", "atcc"));
    }

    private static Airport airport(final String icao, final String iata,
                                   final double lat, final double lon) {
        return new Airport(icao, iata, "Airport " + icao, "City", "PT",
                lat, lon, 100, new AirControlArea("PT-AREA", "Area", 40, 42, -9, -7));
    }
}
