package eapli.aisafe.app.user.console.presentation.weather;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.weather.application.ConsultWeatherDataController;
import eapli.aisafe.weather.application.consultation.WeatherConsultationResult;
import eapli.aisafe.weather.application.consultation.WeatherDataSummary;
import eapli.framework.io.util.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** US043 — consult weather observations by area and calendar day. */
public class ConsultWeatherDataUI extends AbstractBaseUI {

    private final ConsultWeatherDataController controller = new ConsultWeatherDataController();

    @Override
    protected boolean doShow() {
        final String areaCode = Console.readLine("Air control area code: ");
        final String dateStr = Console.readLine("Date (yyyy-MM-dd, UTC day): ");

        final LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (final DateTimeParseException ex) {
            System.out.println("Invalid date. Use format yyyy-MM-dd (example: 2026-05-29).");
            return false;
        }

        try {
            final WeatherConsultationResult result = controller.consult(areaCode, date);
            printResult(result);
        } catch (final IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    private static void printResult(final WeatherConsultationResult result) {
        if (result.recordCount() == 0) {
            System.out.println("No weather data found for "
                    + result.areaCode() + " on " + result.date() + ".");
            return;
        }

        System.out.println("\n--- Weather data for " + result.areaCode()
                + " on " + result.date() + " (" + result.recordCount() + " record(s)) ---");
        for (final WeatherDataSummary row : result.records()) {
            System.out.printf(
                    "  %s | temp=%.1f C | humidity=%.0f%% | wind=%.1f m/s @ %.0f deg | source=%s%n",
                    row.observedAt(),
                    row.temperatureC(),
                    row.humidityPercent(),
                    row.windSpeedMs(),
                    row.windDirectionDegrees(),
                    row.source());
        }
        System.out.println("----------------------------------------------------------------");
    }

    @Override
    public String headline() {
        return super.headline() + " > Consult weather data";
    }
}
