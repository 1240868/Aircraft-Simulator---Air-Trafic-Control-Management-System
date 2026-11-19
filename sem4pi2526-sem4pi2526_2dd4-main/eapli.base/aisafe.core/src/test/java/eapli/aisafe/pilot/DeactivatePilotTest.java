package eapli.aisafe.pilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Domain invariants for pilot deactivation.
 * Focus: status transition and flight-plan assignment guard (not controller orchestration).
 */
@DisplayName("Pilot — deactivation domain")
class DeactivatePilotTest {

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
    @DisplayName("[INV-1] active pilot without assigned flight plans becomes INACTIVE")
    void deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans() {
        // Arrange
        final Pilot pilot = activePilot();

        // Act
        pilot.deactivate(false);

        // Assert
        assertEquals(PilotStatus.INACTIVE, pilot.getStatus());
    }

    @Test
    @DisplayName("[INV-1] rejects deactivation when pilot has assigned flight plans")
    void deactivateShouldRejectWhenPilotHasAssignedFlightPlans() {
        // Arrange
        final Pilot pilot = activePilot();

        // Act & Assert
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pilot.deactivate(true));
        assertTrue(ex.getMessage().toLowerCase().contains("flight plan"));
        assertEquals(PilotStatus.ACTIVE, pilot.getStatus());
    }

    @Test
    @DisplayName("[INV-2] only ACTIVE pilots can be deactivated")
    void deactivateShouldRejectWhenPilotIsNotActive() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.SUSPENDED);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pilot.deactivate(false));
        assertEquals(PilotStatus.SUSPENDED, pilot.getStatus());
    }

    @Test
    @DisplayName("[INV-2] RETIRED pilot cannot be deactivated")
    void deactivateShouldRejectWhenPilotIsRetired() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.RETIRED);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pilot.deactivate(false));
        assertEquals(PilotStatus.RETIRED, pilot.getStatus());
    }

    @Test
    @DisplayName("[INV-2] already INACTIVE pilot cannot be deactivated again")
    void deactivateShouldRejectWhenPilotAlreadyInactive() {
        // Arrange
        final Pilot pilot = activePilot();
        pilot.setStatus(PilotStatus.INACTIVE);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> pilot.deactivate(false));
        assertEquals(PilotStatus.INACTIVE, pilot.getStatus());
    }
}
