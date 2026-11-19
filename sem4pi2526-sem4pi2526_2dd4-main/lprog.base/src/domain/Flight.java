package domain;
import java.util.List;
import java.util.Objects;

public class Flight {
    private final String id;
    private final FlightType type;
    private final Route route;
    private final List<Leg> legs;

    public Flight(String id, FlightType type, Route route, List<Leg> legs) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.route = route; // may be null if route is optional in your variant
        this.legs = List.copyOf(legs);
    }

    public String id() { return id; }
    public FlightType type() { return type; }
    public Route route() { return route; }
    public List<Leg> legs() { return legs; }
}
