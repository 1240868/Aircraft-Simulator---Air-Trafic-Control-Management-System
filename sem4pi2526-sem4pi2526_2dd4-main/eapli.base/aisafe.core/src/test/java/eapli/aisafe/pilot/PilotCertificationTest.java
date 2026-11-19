package eapli.aisafe.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PilotCertificationTest {

    @Test
    public void shouldCreateValidCertification() {
        final PilotCertification cert = new PilotCertification("ATPL");
        assertNotNull(cert);
        assertEquals("ATPL", cert.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyValue() {
        new PilotCertification("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankValue() {
        new PilotCertification("   ");
    }

    @Test
    public void shouldTrimValue() {
        final PilotCertification cert = new PilotCertification("  CPL  ");
        assertEquals("CPL", cert.value());
    }

    @Test
    public void shouldAcceptMultiWordCertification() {
        final PilotCertification cert = new PilotCertification("CPL(A) IR ME");
        assertEquals("CPL(A) IR ME", cert.value());
    }
}
