package eapli.aisafe.flightPlan.application;

import java.time.LocalDateTime;
import java.util.List;

public record FlightPlanDTO(
        String flightRouteId,
        String aircraftReg,
        String pilotId,
        String designator,
        String flightType,
        LocalDateTime departureDateTime,
        List<LegDTO> legs,
        WindDTO wind) {

    public record LegDTO(String departureAirport, String arrivalAirport, int legOrder,
                         List<String> segments, FuelPlanDTO fuelPlan) {
    }

    public record WindDTO(double speed, String speedUnit, double directionDegrees) {
    }

    public record FuelPlanDTO(double totalFuel, double reserveFuel, String fuelUnit) {
    }
}
