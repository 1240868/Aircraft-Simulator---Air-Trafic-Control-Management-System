package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import org.junit.Test;

public class AirOperatorCertificateTest {

    private static final LocalDate FUTURE = LocalDate.of(2030, 12, 31);

    @Test
    public void shouldCreateValidCertificate() {
        final var cert = new AirOperatorCertificate("AOC-001", FUTURE);
        assertNotNull(cert);
    }

    @Test
    public void shouldTrimCertificateNumber() {
        final var cert = new AirOperatorCertificate("  AOC-002  ", FUTURE);
        assertEquals("AOC-002", cert.number());
    }

    @Test
    public void numberShouldReturnCertificateNumber() {
        final var cert = new AirOperatorCertificate("AOC-001", FUTURE);
        assertEquals("AOC-001", cert.number());
    }

    @Test
    public void expiryDateShouldReturnDate() {
        final var cert = new AirOperatorCertificate("AOC-001", FUTURE);
        assertEquals(FUTURE, cert.expiryDate());
    }

    @Test
    public void shouldRejectNullNumber() {
        assertThrows(Exception.class,
                () -> new AirOperatorCertificate(null, FUTURE));
    }

    @Test
    public void shouldRejectEmptyNumber() {
        assertThrows(Exception.class,
                () -> new AirOperatorCertificate("", FUTURE));
    }

    @Test
    public void shouldRejectBlankNumber() {
        assertThrows(Exception.class,
                () -> new AirOperatorCertificate("   ", FUTURE));
    }

    @Test
    public void shouldRejectNullExpiryDate() {
        assertThrows(NullPointerException.class,
                () -> new AirOperatorCertificate("AOC-001", null));
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final Constructor<AirOperatorCertificate> ctor =
                AirOperatorCertificate.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var cert = ctor.newInstance();
        assertNotNull(cert);
    }
}
