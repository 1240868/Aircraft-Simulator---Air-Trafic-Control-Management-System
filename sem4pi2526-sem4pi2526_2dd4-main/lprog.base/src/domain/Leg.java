package domain;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class Leg {
    private final AirportCode departureAirport;
    private final LocalDate departureDate;
    private final LocalTime departureTime;
    private final AirportCode arrivalAirport;
    private final LocalDate arrivalDate;
    private final LocalTime arrivalTime;
    private final List<Segment> segments;
    private final Fuel fuel;
    private final int line; // for error reporting

    public Leg(AirportCode departureAirport, LocalDate departureDate, LocalTime departureTime,
               AirportCode arrivalAirport, LocalDate arrivalDate, LocalTime arrivalTime,
               List<Segment> segments, Fuel fuel, int line) {
        this.departureAirport = Objects.requireNonNull(departureAirport);
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.arrivalAirport = Objects.requireNonNull(arrivalAirport);
        this.arrivalDate = arrivalDate;
        this.arrivalTime = arrivalTime;
        this.segments = List.copyOf(segments);
        this.fuel = fuel;
        this.line = line;
    }

    public AirportCode departureAirport() { return departureAirport; }
    public LocalDate departureDate() { return departureDate; }
    public LocalTime departureTime() { return departureTime; }
    public AirportCode arrivalAirport() { return arrivalAirport; }
    public LocalDate arrivalDate() { return arrivalDate; }
    public LocalTime arrivalTime() { return arrivalTime; }
    public List<Segment> segments() { return segments; }
    public Fuel fuel() { return fuel; }
    public int line() { return line; }
}
