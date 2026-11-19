package eapli.aisafe.company.application;

/**
 * Presentation-friendly option for selecting an air transport company (no domain types exposed).
 */
public record AirTransportCompanyOptionDTO(String companyId, String displayName, String iataCode) {
}
