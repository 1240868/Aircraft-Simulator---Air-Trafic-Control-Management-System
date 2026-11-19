package eapli.aisafe.reporting.application;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanStatus;
import eapli.aisafe.flightPlan.domain.FlightType;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.reporting.domain.MonthlyReportStats;
import eapli.aisafe.reporting.domain.ReportDataCollector;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MonthlyReportDataCollector implements ReportDataCollector<MonthlyReportStats> {

    private final FlightPlanRepository flightPlanRepo;
    private final AirportRepository airportRepo;
    private final AirControlAreaCode areaCode;
    private final int year;
    private final int month;

    public MonthlyReportDataCollector(final FlightPlanRepository flightPlanRepo,
                                       final AirportRepository airportRepo,
                                       final AirControlAreaCode areaCode,
                                       final int year, final int month) {
        this.flightPlanRepo = flightPlanRepo;
        this.airportRepo = airportRepo;
        this.areaCode = areaCode;
        this.year = year;
        this.month = month;
    }

    @Override
    public MonthlyReportStats collect() {
        final Set<String> areaAirportCodes = airportIcaoCodesForArea();
        final Set<String> pilotIds = new HashSet<>();
        final Set<String> aircraftIds = new HashSet<>();

        int total = 0, approved = 0, rejected = 0, draft = 0, submitted = 0,
                completed = 0, cancelled = 0, active = 0, pendingTest = 0, tested = 0;
        int commercial = 0, cargo = 0, charter = 0, privateF = 0, military = 0, emergency = 0;

        for (final FlightPlan fp : flightPlanRepo.findByDepartureYearMonth(year, month)) {
            if (!departsFromArea(fp, areaAirportCodes)) {
                continue;
            }
            total++;

            final FlightPlanStatus status = fp.getStatus();
            if (status == FlightPlanStatus.APPROVED)       approved++;
            else if (status == FlightPlanStatus.REJECTED)  rejected++;
            else if (status == FlightPlanStatus.DRAFT)     draft++;
            else if (status == FlightPlanStatus.SUBMITTED) submitted++;
            else if (status == FlightPlanStatus.COMPLETED) completed++;
            else if (status == FlightPlanStatus.CANCELLED) cancelled++;
            else if (status == FlightPlanStatus.ACTIVE)    active++;
            else if (status == FlightPlanStatus.PENDING_TEST) pendingTest++;
            else if (status == FlightPlanStatus.TESTED)    tested++;

            final FlightType type = fp.getFlightType();
            if (type == FlightType.COMMERCIAL)      commercial++;
            else if (type == FlightType.CARGO)      cargo++;
            else if (type == FlightType.CHARTER)    charter++;
            else if (type == FlightType.PRIVATE)    privateF++;
            else if (type == FlightType.MILITARY)   military++;
            else if (type == FlightType.EMERGENCY)  emergency++;

            if (fp.getPilot() != null)   pilotIds.add(fp.getPilot().toString());
            if (fp.getAircraft() != null) aircraftIds.add(fp.getAircraft().toString());
        }

        return MonthlyReportStats.builder()
                .total(total)
                .approved(approved).rejected(rejected).draft(draft)
                .submitted(submitted).completed(completed).cancelled(cancelled)
                .active(active).pendingTest(pendingTest).tested(tested)
                .commercial(commercial).cargo(cargo).charter(charter)
                .privateFlights(privateF).military(military).emergency(emergency)
                .distinctPilots(pilotIds.size())
                .distinctAircraft(aircraftIds.size())
                .build();
    }

    private Set<String> airportIcaoCodesForArea() {
        final Set<String> codes = new HashSet<>();
        for (final Airport a : airportRepo.findByAirControlAreaCode(areaCode)) {
            codes.add(a.icaoCode());
        }
        return codes;
    }

    private boolean departsFromArea(final FlightPlan fp, final Set<String> areaAirportCodes) {
        final List<FlightLeg> legs = fp.getLegs();
        if (legs == null || legs.isEmpty()) {
            return false;
        }
        return areaAirportCodes.contains(legs.get(0).departureAirport().code());
    }
}
