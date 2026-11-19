package eapli.aisafe.app.user.console.presentation.weather;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.weather.application.RegisterWeatherDataController;
import eapli.framework.io.util.Console;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.ZoneOffset;

public class RegisterWeatherDataUI extends AbstractBaseUI {
    private final RegisterWeatherDataController controller = new RegisterWeatherDataController();

    @Override
    protected boolean doShow() {
        final String areaCode = Console.readLine("Air control area code: ");
        final String observedAtStr = Console.readLine("Observed at (yyyy-MM-ddTHH:mm, UTC): ");

        final Instant observedAt;
        try {
            final LocalDateTime ldt = LocalDateTime.parse(observedAtStr.trim());
            observedAt = ldt.toInstant(ZoneOffset.UTC);
        } catch (final DateTimeParseException ex) {
            System.out.println("Invalid date/time. Use format yyyy-MM-ddTHH:mm (example: 2026-05-11T14:30).");
            return false;
        }

        final double temperatureC = Console.readDouble("Temperature (C): ");
        final double humidityPercent = Console.readDouble("Humidity (%): ");
        final double windSpeed = Console.readDouble("Wind speed (m/s): ");
        final double windDirection = Console.readDouble("Wind direction (degrees 0-359): ");

        try {
            final var wd = controller.register(
                areaCode,
                observedAt,
                temperatureC,
                humidityPercent,
                windSpeed,
                windDirection
            );
            System.out.println("Weather data registered: " + wd.identity());
        } catch (final IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Register weather data";
    }
}

