package eapli.aisafe.pilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain invariants for pilot roster listing.
 * Focus: aggregate eligibility rules only (not getter/DTO mapping — covered by controller tests).
 */
@DisplayName("Pilot — roster listing domain")
class PilotRosterListingTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private static final LocalDateTime FUTURE = LocalDateTime.now().plusYears(2);
    private static final List<AircraftModelId> MODELS = List.of(AircraftModelId.newId());

    private Pilot activePilot() {
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
    @DisplayName("[INV-1] newly created pilot is eligible for roster listing")
    void activePilotShouldBeEligibleForRosterListing() {
        // Arrange
        final Pilot pilot = activePilot();

        // Act & Assert
        assertTrue(pilot.isEligibleForRosterListing());
    }

    @Test
    @DisplayName("[INV-1] inactive pilot is excluded from roster listing")
    void inactivePilotShouldNotBeEligibleForRosterListing() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.INACTIVE);

        // Act & Assert
        assertFalse(pilot.isEligibleForRosterListing());
    }

    @Test
    @DisplayName("[INV-1] suspended pilot is excluded from roster listing")
    void suspendedPilotShouldNotBeEligibleForRosterListing() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.SUSPENDED);

        // Act & Assert
        assertFalse(pilot.isEligibleForRosterListing());
    }

    @Test
    @DisplayName("[INV-1] retired pilot is excluded from roster listing")
    void retiredPilotShouldNotBeEligibleForRosterListing() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.RETIRED);

        // Act & Assert
        assertFalse(pilot.isEligibleForRosterListing());
    }

    @Test
    @DisplayName("[INV-2] certified models list is unmodifiable within aggregate boundary")
    void certifiedModelsShouldBeUnmodifiable() {
        // Arrange
        final Pilot pilot = activePilot();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class,
                () -> pilot.getCertifiedModels().clear());
    }
}
