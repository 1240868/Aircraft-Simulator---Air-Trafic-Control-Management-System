package eapli.aisafe.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PilotTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);
    private static final List<AircraftModelId> MODELS = List.of(AircraftModelId.newId());

    private Pilot validPilot() {
        return new Pilot(
                PilotId.newId(),
                new FullName("Ana", "Silva"),
                new Email("ana.silva@tap.pt"),
                COMPANY,
                new PilotCertification("ATPL"),
                new License("PT-LIC-001", FUTURE),
                MODELS);
    }

    @Test
    public void shouldCreateActiveStatus() {
        assertEquals(PilotStatus.ACTIVE, validPilot().getStatus());
    }

    @Test
    public void identityShouldReturnId() {
        final PilotId id = PilotId.newId();
        final Pilot pilot = new Pilot(id, new FullName("Ana", "Silva"),
                new Email("ana@tap.pt"), COMPANY,
                new PilotCertification("ATPL"),
                new License("LIC-001", FUTURE), MODELS);
        assertEquals(id, pilot.identity());
    }

    @Test
    public void getNameShouldReturnProvidedName() {
        final Pilot pilot = validPilot();
        assertEquals("Ana", pilot.getName().firstName());
        assertEquals("Silva", pilot.getName().lastName());
    }

    @Test
    public void getEmailShouldReturnNormalisedEmail() {
        final Pilot pilot = validPilot();
        assertEquals("ana.silva@tap.pt", pilot.getEmail().value());
    }

    @Test
    public void getCompanyShouldReturnProvidedCompany() {
        assertEquals(COMPANY, validPilot().getCompany());
    }

    @Test
    public void getCertificationShouldReturnProvidedValue() {
        assertEquals("ATPL", validPilot().getCertification().value());
    }

    @Test
    public void getLicenseShouldReturnProvidedLicense() {
        assertNotNull(validPilot().getLicenseVoo());
        assertEquals("PT-LIC-001", validPilot().getLicenseVoo().value());
    }

    @Test
    public void getCertifiedModelsShouldReturnProvidedModels() {
        final Pilot pilot = validPilot();
        assertEquals(1, pilot.getCertifiedModels().size());
        assertEquals(MODELS.get(0), pilot.getCertifiedModels().get(0));
    }

    @Test
    public void shouldAllowMultipleCertifiedModels() {
        final List<AircraftModelId> twoModels = List.of(AircraftModelId.newId(), AircraftModelId.newId());
        final Pilot pilot = new Pilot(PilotId.newId(), new FullName("Ana", "Silva"),
                new Email("ana@tap.pt"), COMPANY,
                new PilotCertification("ATPL"),
                new License("LIC-001", FUTURE), twoModels);
        assertEquals(2, pilot.getCertifiedModels().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyCertifiedModels() {
        new Pilot(PilotId.newId(), new FullName("Ana", "Silva"),
                new Email("ana@tap.pt"), COMPANY,
                new PilotCertification("ATPL"),
                new License("LIC-001", FUTURE), new ArrayList<>());
    }

    @Test
    public void sameAsSelfShouldReturnTrue() {
        final Pilot p = validPilot();
        assertTrue(p.sameAs(p));
    }

    @Test
    public void sameAsPilotWithSameEmailShouldReturnTrue() {
        final Pilot a = new Pilot(PilotId.newId(), new FullName("Ana", "Silva"),
                new Email("shared@tap.pt"), COMPANY,
                new PilotCertification("ATPL"), new License("L1", FUTURE), MODELS);
        final Pilot b = new Pilot(PilotId.newId(), new FullName("Rui", "Costa"),
                new Email("shared@tap.pt"), new CompanyId(UUID.randomUUID()),
                new PilotCertification("CPL"), new License("L2", FUTURE), MODELS);
        assertTrue(a.sameAs(b));
    }

    @Test
    public void sameAsPilotWithDifferentEmailShouldReturnFalse() {
        final Pilot a = validPilot();
        final Pilot b = new Pilot(PilotId.newId(), new FullName("Rui", "Costa"),
                new Email("rui@tap.pt"), COMPANY,
                new PilotCertification("ATPL"), new License("L2", FUTURE), MODELS);
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsNullShouldReturnFalse() {
        assertFalse(validPilot().sameAs(null));
    }

    @Test
    public void sameAsDifferentTypeShouldReturnFalse() {
        assertFalse(validPilot().sameAs("not a pilot"));
    }

    @Test
    public void equalsByIdShouldReturnTrueForSameId() {
        final PilotId id = PilotId.newId();
        final Pilot a = new Pilot(id, new FullName("A", "B"),
                new Email("a@tap.pt"), COMPANY,
                new PilotCertification("ATPL"), new License("L1", FUTURE), MODELS);
        final Pilot b = new Pilot(id, new FullName("C", "D"),
                new Email("c@tap.pt"), COMPANY,
                new PilotCertification("CPL"), new License("L2", FUTURE), MODELS);
        assertEquals(a, b);
    }

    @Test
    public void setStatusShouldUpdateStatus() {
        final Pilot p = validPilot();
        p.setStatus(PilotStatus.INACTIVE);
        assertEquals(PilotStatus.INACTIVE, p.getStatus());
    }
}
