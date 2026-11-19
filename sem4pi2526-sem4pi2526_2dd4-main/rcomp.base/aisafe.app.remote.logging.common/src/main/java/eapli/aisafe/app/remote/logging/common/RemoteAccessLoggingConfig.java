package eapli.aisafe.app.remote.logging.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Loads remote logging target host/port from system properties or classpath properties. */
public final class RemoteAccessLoggingConfig {

    private static final String HOST_KEY = "remote.logging.host";
    private static final String PORT_KEY = "remote.logging.port";
    private static final String HTTP_PORT_KEY = "remote.http.port";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 9990;
    private static final int DEFAULT_HTTP_PORT = 8080;

    private RemoteAccessLoggingConfig() {
    }

    public static String host() {
        return firstNonBlank(
                System.getenv("REMOTE_LOGGING_HOST"),
                System.getProperty(HOST_KEY),
                propertyFromClasspath(HOST_KEY),
                DEFAULT_HOST);
    }

    public static int port() {
        final String raw = firstNonBlank(
                System.getenv("REMOTE_LOGGING_PORT"),
                System.getProperty(PORT_KEY),
                propertyFromClasspath(PORT_KEY),
                String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(Objects.requireNonNull(raw).trim());
        } catch (final NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    public static int httpPort() {
        final String raw = firstNonBlank(
                System.getenv("REMOTE_HTTP_PORT"),
                System.getProperty(HTTP_PORT_KEY),
                propertyFromClasspath(HTTP_PORT_KEY),
                String.valueOf(DEFAULT_HTTP_PORT));
        try {
            return Integer.parseInt(Objects.requireNonNull(raw).trim());
        } catch (final NumberFormatException e) {
            return DEFAULT_HTTP_PORT;
        }
    }

    /** Applies entries from a classpath properties resource to system properties. */
    public static void loadClasspathProperties(final String resourceName) {
        try (InputStream in = RemoteAccessLoggingConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                return;
            }
            final Properties props = new Properties();
            props.load(in);
            for (final String key : props.stringPropertyNames()) {
                if (System.getProperty(key) == null) {
                    System.setProperty(key, props.getProperty(key));
                }
            }
        } catch (final IOException e) {
            System.err.println("Could not load remote logging properties: " + e.getMessage());
        }
    }

    private static String propertyFromClasspath(final String key) {
        try (InputStream in = RemoteAccessLoggingConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                return null;
            }
            final Properties props = new Properties();
            props.load(in);
            return props.getProperty(key);
        } catch (final IOException e) {
            return null;
        }
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
