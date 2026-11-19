package domain;

public class AltitudeSlot {
    private final double altitude;
    private final String altitudeUnit;
    private final double width;
    private final String widthUnit;
    private final int line;

    public AltitudeSlot(double altitude, String altitudeUnit,
                        double width, String widthUnit, int line) {
        this.altitude = altitude;
        this.altitudeUnit = altitudeUnit;
        this.width = width;
        this.widthUnit = widthUnit;
        this.line = line;
    }

    public double altitude() { return altitude; }
    public String altitudeUnit() { return altitudeUnit; }
    public double width() { return width; }
    public String widthUnit() { return widthUnit; }
    public int line() { return line; }
}
