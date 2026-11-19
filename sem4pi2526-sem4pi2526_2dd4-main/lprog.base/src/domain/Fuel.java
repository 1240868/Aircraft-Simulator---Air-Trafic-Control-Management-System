package domain;

public class Fuel {
    private final double quantity;
    private final String unit;
    private final int line;

    public Fuel(double quantity, String unit, int line) {
        this.quantity = quantity;
        this.unit = unit;
        this.line = line;
    }

    public double quantity() { return quantity; }
    public String unit() { return unit; }
    public int line() { return line; }
}
