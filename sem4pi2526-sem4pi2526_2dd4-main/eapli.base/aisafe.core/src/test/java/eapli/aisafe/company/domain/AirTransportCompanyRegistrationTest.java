package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Test;

/**
 * US060 — registo de empresa de transporte aéreo: agregado no fluxo de criação.
 * Colaboradores e invariantes extensas estão em {@link AirTransportCompanyCollaboratorTest}.
 */
public class AirTransportCompanyRegistrationTest {

    private static String letters2() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "AB").substring(0, 2);
    }

    private static String letters3() {
        final String u = UUID.randomUUID().toString().toUpperCase().replaceAll("[^A-Z]", "");
        return (u + "ABC").substring(0, 3);
    }

    private static AirOperatorCertificate cert() {
        return new AirOperatorCertificate("AOC-" + letters2(), LocalDate.of(2031, 1, 15));
    }

    @Test
    public void newCompanyShouldHaveIdentityAndExposeValueObjects() {
        final String iata = letters2();
        final String icao = letters3();
        final var name = new CompanyName("Regional Air " + iata);
        final var ao = new AirOperatorCertificate("AOC-REG-1", LocalDate.of(2031, 1, 15));
        final var c = new AirTransportCompany(
                name,
                new CompanyIataCode(iata),
                new CompanyIcaoCode(icao),
                ao);

        assertNotNull(c.identity());
        assertEquals(name.value(), c.name().value());
        assertEquals(iata, c.iataCode().value());
        assertEquals(icao, c.icaoCode().value());
        assertEquals("AOC-REG-1", c.certificate().number());
        assertEquals(LocalDate.of(2031, 1, 15), c.certificate().expiryDate());
    }

    @Test
    public void twoNewCompaniesShouldHaveDistinctIdentities() {
        final var a = new AirTransportCompany(
                new CompanyName("A Lines"),
                new CompanyIataCode(letters2()),
                new CompanyIcaoCode(letters3()),
                cert());
        final var b = new AirTransportCompany(
                new CompanyName("B Lines"),
                new CompanyIataCode(letters2()),
                new CompanyIcaoCode(letters3()),
                cert());
        assertNotEquals(a.identity(), b.identity());
    }

    @Test
    public void iataCodeShouldNormalizeToUpperCase() {
        final var c = new AirTransportCompany(
                new CompanyName("Case Co"),
                new CompanyIataCode("  ab  "),
                new CompanyIcaoCode(letters3()),
                cert());
        assertEquals("AB", c.iataCode().value());
    }
}
