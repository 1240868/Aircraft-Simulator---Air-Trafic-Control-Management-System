package parsing;

public class Listener extends parsing.FlightDSLBaseListener {
    private int indent = 0;

    private void print(String s) {
        System.out.println("  ".repeat(indent) + s);
    }

    @Override
    public void enterFlight(parsing.FlightDSLParser.FlightContext ctx) {
        print("Flight: " + ctx.id.getText() + " (line " + ctx.getStart().getLine() + ")");
        indent++;
    }

    @Override
    public void exitFlight(parsing.FlightDSLParser.FlightContext ctx) { indent--; }

    @Override
    public void enterLeg(parsing.FlightDSLParser.LegContext ctx) {
        print("Leg (line " + ctx.getStart().getLine() + ")");
        indent++;
    }

    @Override
    public void exitLeg(parsing.FlightDSLParser.LegContext ctx) { indent--; }

    @Override
    public void enterSegment(parsing.FlightDSLParser.SegmentContext ctx) {
        print("Segment (line " + ctx.getStart().getLine() + ")");
        indent++;
    }

    @Override
    public void exitSegment(parsing.FlightDSLParser.SegmentContext ctx) { indent--; }
}
