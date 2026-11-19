package domain;

public class FlightPlan {
    private final Flight flight;

    public FlightPlan(Flight flight) {
        this.flight = flight;
    }

    public Flight flight() { return flight; }
}
