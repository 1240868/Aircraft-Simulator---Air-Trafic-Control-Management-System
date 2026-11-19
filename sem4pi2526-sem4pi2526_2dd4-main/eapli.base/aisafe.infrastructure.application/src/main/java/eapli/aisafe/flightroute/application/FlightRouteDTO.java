package eapli.aisafe.flightroute.application;

/** Result of creating a flight route (US073). */
public record FlightRouteDTO(
        String routeId,
        String routeName,
        String companyName,
        String originAirportIcao,
        String originAirportIata,
        String destinationAirportIcao,
        String destinationAirportIata,
        double distanceKm,
        long durationMinutes,
        String status
) {
}
