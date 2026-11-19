package eapli.aisafe.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.time.LocalDateTime;

public class LicenseTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(1);

    @Test
    public void shouldCreateValidLicense() {
        final License lic = new License("PT-001", FUTURE);
        assertNotNull(lic);
        assertEquals("PT-001", lic.value());
        assertEquals(FUTURE, lic.expirationDate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyNumber() {
        new License("", FUTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankNumber() {
        new License("   ", FUTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullExpiration() {
        new License("PT-001", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectPastExpiration() {
        new License("PT-001", LocalDateTime.now().minusDays(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectExpirationAtPresentMoment() {
        new License("PT-001", LocalDateTime.now().minusNanos(1));
    }

    @Test
    public void shouldTrimLicenseNumber() {
        final License lic = new License("  PT-002  ", FUTURE);
        assertEquals("PT-002", lic.value());
    }
}
