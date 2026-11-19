package eapli.aisafe.weather.application.importing;

/** Raised when a bulk import file cannot be parsed (US042). */
public class ImportParseException extends RuntimeException {

    public ImportParseException(final String message) {
        super(message);
    }

    public ImportParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
