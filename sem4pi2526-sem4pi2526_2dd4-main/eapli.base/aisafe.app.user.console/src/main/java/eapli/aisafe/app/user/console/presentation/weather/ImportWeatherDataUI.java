package eapli.aisafe.app.user.console.presentation.weather;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.weather.application.ImportWeatherDataController;
import eapli.aisafe.weather.application.importing.ImportParseException;
import eapli.aisafe.weather.application.importing.UnsupportedImportFormatException;
import eapli.aisafe.weather.application.importing.WeatherImportResult;
import eapli.aisafe.weather.application.importing.WeatherImportRowError;
import eapli.framework.io.util.Console;
import java.nio.file.Path;

/** US042 — bulk import of weather observations from a file. */
public class ImportWeatherDataUI extends AbstractBaseUI {

    private final ImportWeatherDataController controller = new ImportWeatherDataController();

    @Override
    protected boolean doShow() {
        try {
            final var formats = controller.supportedFormats();
            System.out.println("Supported import formats: " + String.join(", ", formats));
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        final String format = Console.readLine("Import format [CSV]: ");
        final String pathStr = Console.readLine("File path: ");
        if (pathStr == null || pathStr.isBlank()) {
            System.out.println("File path is required.");
            return false;
        }

        try {
            final WeatherImportResult result = controller.importFromFile(
                    format == null || format.isBlank() ? "CSV" : format,
                    Path.of(pathStr.trim()));
            printSummary(result);
        } catch (final UnsupportedImportFormatException | ImportParseException ex) {
            System.out.println(ex.getMessage());
            return false;
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return false;
    }

    private static void printSummary(final WeatherImportResult result) {
        System.out.println("--- Import summary ---");
        System.out.println("Total rows:    " + result.totalRows());
        System.out.println("Imported:      " + result.importedCount());
        System.out.println("Rejected:      " + result.rejectedCount());
        if (!result.errors().isEmpty()) {
            System.out.println("Row errors:");
            for (final WeatherImportRowError err : result.errors()) {
                System.out.println("  Line " + err.lineNumber() + ": " + err.message());
            }
        }
    }

    @Override
    public String headline() {
        return super.headline() + " > Import bulk weather data";
    }
}
