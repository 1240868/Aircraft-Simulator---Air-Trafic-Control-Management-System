package eapli.aisafe.flightroute.application;

/** Airport option for route creation UI (US073). */
public record OperationalAirportDTO(
        String icaoCode,
        String iataCode,
        String name,
        String town,
        String country
) {
}
