package domain;

public class Coordinates {
    private final double latitude;
    private final double longitude;
    private final int line;

    public Coordinates(double latitude, double longitude, int line) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.line = line;
    }

    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
    public int line() { return line; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates c)) return false;
        return Double.compare(c.latitude, latitude) == 0
                && Double.compare(c.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
    }
}
