package eapli.aisafe.app.remote.logging.server;

import eapli.aisafe.app.remote.logging.common.RemoteAccessLoggingConfig;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Remote Accesses Logging Server — dedicated UDP node for remote access auditing. */
public final class RemoteAccessLoggingServer {

    private static final String LOG_FILE_PROPERTY = "remote.logging.file";
    private static final String DEFAULT_LOG_FILE = "remote-access-events.log";

    public static void main(final String[] args) {
        RemoteAccessLoggingConfig.loadClasspathProperties("application.properties");

        final int listenPort = RemoteAccessLoggingConfig.port();
        final int httpPort = RemoteAccessLoggingConfig.httpPort();
        final Path logFile = resolveLogFile();

        final RemoteAccessLogStore store = new RemoteAccessLogStore();

        final Thread receiverThread = new Thread(
                new RemoteAccessUdpReceiver(listenPort, store, logFile),
                "remote-access-udp-receiver");
        receiverThread.setDaemon(false);
        receiverThread.start();

        final Thread httpThread = new Thread(
                new RemoteAccessHttpServer(httpPort, store),
                "remote-access-http-server");
        httpThread.setDaemon(false);
        httpThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            receiverThread.interrupt();
            httpThread.interrupt();
        }));

        System.out.println("Remote access events file: " + logFile.toAbsolutePath());
        System.out.println("Status pages: http://localhost:" + httpPort + "/");
        System.out.println("Press Ctrl+C to stop.");
    }

    private static Path resolveLogFile() {
        final String configured = firstNonBlank(
                System.getenv("REMOTE_LOGGING_FILE"),
                System.getProperty(LOG_FILE_PROPERTY),
                DEFAULT_LOG_FILE);
        return Paths.get(configured);
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return DEFAULT_LOG_FILE;
    }

    private RemoteAccessLoggingServer() {
    }
}
