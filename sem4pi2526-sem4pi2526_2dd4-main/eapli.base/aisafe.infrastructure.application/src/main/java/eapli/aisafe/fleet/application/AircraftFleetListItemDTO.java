package eapli.aisafe.fleet.application;

/**
 * One row in a fleet listing (US072). {@code ageInCompleteYears} is set only for list-by-age.
 */
public record AircraftFleetListItemDTO(String registrationNumber,
                                       String modelName,
                                       String manufacturer,
                                       String operationalStatus,
                                       int numberOfSeats,
                                       Integer ageInCompleteYears) {
}
