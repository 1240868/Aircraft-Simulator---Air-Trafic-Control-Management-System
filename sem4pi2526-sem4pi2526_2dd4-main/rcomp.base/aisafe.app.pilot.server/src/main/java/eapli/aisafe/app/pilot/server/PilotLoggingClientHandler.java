package eapli.aisafe.app.pilot.server;

import eapli.aisafe.app.remote.logging.common.RemoteAccessCommandObserver;
import eapli.aisafe.app.remote.logging.common.RemoteAccessLoggingConfig;
import eapli.aisafe.app.remote.logging.common.RemoteAccessServiceId;
import eapli.aisafe.app.remote.logging.common.RemoteAccessSessionTracker;
import eapli.aisafe.app.remote.logging.common.RemoteAccessUdpLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * TCP client handler for pilot remote access with external logging via the shared UDP component.
 * Uses {@link RemoteAccessCommandObserver} so {@link PilotSessionHandler} remains unchanged.
 */
public final class PilotLoggingClientHandler implements Runnable {

    private final Socket socket;
    private final PilotSessionHandler sessionHandler;
    private final RemoteAccessUdpLogger accessLogger;
    private final RemoteAccessSessionTracker sessionTracker = new RemoteAccessSessionTracker();

    public PilotLoggingClientHandler(final Socket socket) {
        this(socket,
                new PilotSessionHandler(
                        socket.getInetAddress().getHostAddress(),
                        socket.getPort(),
                        new eapli.aisafe.app.pilot.server.RemoteAccessUdpLogger("127.0.0.1", 0)),
                new RemoteAccessUdpLogger(RemoteAccessServiceId.PILOT));
    }

    PilotLoggingClientHandler(final Socket socket,
                              final PilotSessionHandler sessionHandler,
                              final RemoteAccessUdpLogger accessLogger) {
        this.socket = socket;
        this.sessionHandler = sessionHandler;
        this.accessLogger = accessLogger;
    }

    @Override
    public void run() {
        final String clientIp = socket.getInetAddress().getHostAddress();
        final int clientPort = socket.getPort();

        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                final String command = line.trim();
                final String response = sessionHandler.processCommand(command);
                RemoteAccessCommandObserver.observe(
                        command, response, clientIp, clientPort, accessLogger, sessionTracker);
                out.print(response);
                out.print('\n');
                out.flush();
                if (response.startsWith("BYE")) {
                    break;
                }
            }
        } catch (final IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            RemoteAccessCommandObserver.observeDisconnect(clientIp, clientPort, accessLogger, sessionTracker);
            sessionHandler.notifyDisconnect();
            try {
                socket.close();
            } catch (final IOException ignored) {
            }
        }
    }

    public static void bootstrapRemoteLoggingConfig() {
        RemoteAccessLoggingConfig.loadClasspathProperties("application.properties");
        RemoteAccessLoggingConfig.loadClasspathProperties("remote-logging.properties");
    }
}
