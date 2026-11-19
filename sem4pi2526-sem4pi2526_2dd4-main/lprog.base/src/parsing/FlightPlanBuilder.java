package parsing;

import domain.AirportCode;
import domain.AltitudeSlot;
import domain.Coordinates;
import domain.Flight;
import domain.FlightPlan;
import domain.FlightType;
import domain.Fuel;
import domain.Leg;
import domain.Route;
import domain.Segment;
import domain.Wind;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class FlightPlanBuilder extends FlightDSLBaseVisitor<Object> {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public FlightPlan visitFlightPlan(FlightDSLParser.FlightPlanContext ctx) {
        Flight flight = (Flight) visit(ctx.flight());
        return new FlightPlan(flight);
    }

    @Override
    public Flight visitFlight(FlightDSLParser.FlightContext ctx) {
        String id = ctx.id.getText();
        FlightType type = visitFlightType(ctx.flightType());
        Route route = ctx.routeId != null ? new Route(ctx.routeId.getText()) : null;

        List<Leg> legs = new ArrayList<>();
        for (FlightDSLParser.LegContext lc : ctx.leg()) {
            legs.add((Leg) visit(lc));
        }
        return new Flight(id, type, route, legs);
    }

    @Override
    public FlightType visitFlightType(FlightDSLParser.FlightTypeContext ctx) {
        return ctx.REGULAR() != null ? FlightType.REGULAR : FlightType.CHARTER;
    }

    @Override
    public Leg visitLeg(FlightDSLParser.LegContext ctx) {
        var dep = ctx.departure();
        var arr = ctx.arrival();

        AirportCode depAirport = new AirportCode(dep.airportCode().getText(),
                dep.getStart().getLine());
        LocalDate depDate = parseDate(dep.DATE().getText());
        LocalTime depTime = parseTime(dep.TIME().getText());

        AirportCode arrAirport = new AirportCode(arr.airportCode().getText(),
                arr.getStart().getLine());
        LocalDate arrDate = parseDate(arr.DATE().getText());
        LocalTime arrTime = parseTime(arr.TIME().getText());

        List<Segment> segments = new ArrayList<>();
        for (FlightDSLParser.SegmentContext sc : ctx.segment()) {
            segments.add((Segment) visit(sc));
        }

        Fuel fuel = (Fuel) visit(ctx.fuel());

        return new Leg(depAirport, depDate, depTime,
                arrAirport, arrDate, arrTime,
                segments, fuel, ctx.getStart().getLine());
    }

    @Override
    public Segment visitSegment(FlightDSLParser.SegmentContext ctx) {
        Coordinates start = (Coordinates) visit(ctx.startC);
        Coordinates end = (Coordinates) visit(ctx.endC);

        List<AltitudeSlot> slots = new ArrayList<>();
        for (FlightDSLParser.AltitudeSlotContext as : ctx.altitudeSlot()) {
            slots.add((AltitudeSlot) visit(as));
        }

        double windAngle = parseNumber(ctx.windAngle);
        String angleUnit = ctx.unitAngle().getText();
        double windSpeed = parseNumber(ctx.windSpeed);
        String speedUnit = ctx.unitSpeed().getText();

        Wind wind = new Wind(windAngle, angleUnit, windSpeed, speedUnit);

        return new Segment(start, end, slots, wind, ctx.getStart().getLine());
    }

    @Override
    public AltitudeSlot visitAltitudeSlot(FlightDSLParser.AltitudeSlotContext ctx) {
        double alt = parseNumber(ctx.alt);
        String altUnit = ctx.unitLength(0).getText();
        double width = parseNumber(ctx.wid);
        String widthUnit = ctx.unitLength(1).getText();
        return new AltitudeSlot(alt, altUnit, width, widthUnit, ctx.getStart().getLine());
    }

    @Override
    public Coordinates visitCoords(FlightDSLParser.CoordsContext ctx) {
        double lat = parseNumber(ctx.lat);
        double lon = parseNumber(ctx.lon);
        return new Coordinates(lat, lon, ctx.getStart().getLine());
    }

    @Override
    public Fuel visitFuel(FlightDSLParser.FuelContext ctx) {
        double q = parseNumber(ctx.number());
        String unit = ctx.unitMass().getText();
        return new Fuel(q, unit, ctx.getStart().getLine());
    }

    private double parseNumber(FlightDSLParser.NumberContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s, DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalTime parseTime(String s) {
        try {
            return LocalTime.parse(s, TIME_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
