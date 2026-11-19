package eapli.aisafe.app.user.console.presentation.flightPlan;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.flightPlan.application.InsertWeatherDataDTO;
import eapli.aisafe.flightPlan.application.InsertWeatherDataInFlightController;
import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.framework.io.util.Console;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class InsertWeatherDataUI extends AbstractBaseUI {

    private final InsertWeatherDataInFlightController controller = new InsertWeatherDataInFlightController();

    @Override
    protected boolean doShow() {
        try {
            final List<FlightPlan> plans = controller.listPlansForCurrentPilot();
            if (plans.isEmpty()) {
                System.out.println("No available flight plans found for your profile.");
                System.out.println("(Plans must not be CANCELLED or COMPLETED.)");
                return false;
            }

            System.out.println("\n--- Your Flight Plans ---");
            int idx = 1;
            for (final FlightPlan plan : plans) {
                System.out.printf("  %d. %s  id=%s  status=%s%n",
                        idx++,
                        plan.getDesignator(),
                        plan.getId(),
                        plan.getStatus());
            }
            System.out.println("--------------------------");

            final int planChoice = Console.readInteger("Select flight plan number");
            if (planChoice < 1 || planChoice > plans.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final FlightPlan selected = plans.get(planChoice - 1);

            final List<FlightLeg> legs = controller.listLegsForPlan(selected.getId());
            if (legs.isEmpty()) {
                System.out.println("This flight plan has no legs.");
                return false;
            }

            System.out.println("\n--- Legs ---");
            int legIdx = 1;
            for (final FlightLeg leg : legs) {
                System.out.printf("  %d. Leg %d  (dep=%s, arr=%s)%n",
                        legIdx++,
                        leg.legOrder(),
                        leg.departureAirport(),
                        leg.arrivalAirport());
            }
            System.out.println("-------------");

            final int legChoice = Console.readInteger("Select leg number");
            if (legChoice < 1 || legChoice > legs.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final FlightLeg selectedLeg = legs.get(legChoice - 1);

            final String areaCode = Console.readLine("Air control area code (e.g. LPPC): ").trim().toUpperCase();

            final String dateStr = Console.readLine("Observation date/time (yyyy-MM-dd HH:mm): ");
            final Instant observedAt;
            try {
                observedAt = LocalDateTime.parse(dateStr.strip(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant();
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date/time format. Please use yyyy-MM-dd HH:mm.");
                return false;
            }

            final double windSpeed = Console.readDouble("Wind speed (m/s): ");
            final double windDirection = Console.readDouble("Wind direction (degrees, 0-359): ");
            final double temperature = Console.readDouble("Temperature (Celsius): ");
            final double humidity = Console.readDouble("Humidity (%, 0-100): ");

            final InsertWeatherDataDTO dto = new InsertWeatherDataDTO(
                    selected.getId().toString(),
                    selectedLeg.identity().toString(),
                    areaCode,
                    observedAt,
                    windSpeed, windDirection, temperature, humidity);

            controller.insertWeatherData(dto);

            System.out.println("Weather data successfully added to flight plan.");
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Insert Weather Data in Flight Plan";
    }
}