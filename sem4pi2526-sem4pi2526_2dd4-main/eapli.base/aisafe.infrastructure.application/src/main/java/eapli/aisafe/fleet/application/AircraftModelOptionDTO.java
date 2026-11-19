package eapli.aisafe.fleet.application;

/**
 * Presentation-friendly summary of an aircraft model for selection lists.
 */
public record AircraftModelOptionDTO(String modelId,
                                     String modelName,
                                     String manufacturer,
                                     String type,
                                     int passengerCapacity) {
}
