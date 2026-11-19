package eapli.aisafe.weather.application.importing;

import java.io.InputStream;
import java.util.List;

/** Strategy port for format-specific bulk weather file parsing (US042). */
public interface WeatherDataImportParser {

    String supportedFormat();

    List<WeatherImportRow> parse(InputStream input);
}
