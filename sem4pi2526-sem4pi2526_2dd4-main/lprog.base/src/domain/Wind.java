package domain;

public class Wind {
    private final double angle;       // degrees from North
    private final String angleUnit;
    private final double speed;
    private final String speedUnit;

    public Wind(double angle, String angleUnit, double speed, String speedUnit) {
        this.angle = angle;
        this.angleUnit = angleUnit;
        this.speed = speed;
        this.speedUnit = speedUnit;
    }

    public double angle() { return angle; }
    public String angleUnit() { return angleUnit; }
    public double speed() { return speed; }
    public String speedUnit() { return speedUnit; }
}
