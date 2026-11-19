package eapli.aisafe.weather.application.importing;

/** Raised when the requested bulk import format is not registered (US042). */
public class UnsupportedImportFormatException extends RuntimeException {

    public UnsupportedImportFormatException(final String format) {
        super("Unsupported import format: " + format);
    }
}
