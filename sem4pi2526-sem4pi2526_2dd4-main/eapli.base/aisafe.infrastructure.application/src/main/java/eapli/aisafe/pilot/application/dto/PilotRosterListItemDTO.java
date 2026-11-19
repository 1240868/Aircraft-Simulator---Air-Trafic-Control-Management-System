package eapli.aisafe.pilot.application.dto;

import java.util.List;

/** One row in the pilot roster listing. Transport object — no behaviour. */
public record PilotRosterListItemDTO(String pilotId,
                                   String fullName,
                                   String email,
                                   String certification,
                                   String licenseNumber,
                                   String licenseExpiration,
                                   String status,
                                   List<String> certifiedModels) {
}
