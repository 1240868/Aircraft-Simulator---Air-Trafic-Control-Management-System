package domain;

import java.util.List;

public class Segment {
    private final Coordinates start;
    private final Coordinates end;
    private final List<AltitudeSlot> altitudeSlots;
    private final Wind wind;
    private final int line;

    public Segment(Coordinates start, Coordinates end,
                   List<AltitudeSlot> altitudeSlots, Wind wind, int line) {
        this.start = start;
        this.end = end;
        this.altitudeSlots = List.copyOf(altitudeSlots);
        this.wind = wind;
        this.line = line;
    }

    public Coordinates start() { return start; }
    public Coordinates end() { return end; }
    public List<AltitudeSlot> altitudeSlots() { return altitudeSlots; }
    public Wind wind() { return wind; }
    public int line() { return line; }
}
