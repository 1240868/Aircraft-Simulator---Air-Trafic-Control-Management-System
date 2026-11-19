package eapli.aisafe.app.pilot.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Emits UDP datagrams to the Remote Accesses Logging Server. */
final class RemoteAccessUdpLogger {

    static final String SERVICE_ID = "PILOT";

    private final String host;
    private final int port;

    RemoteAccessUdpLogger() {
        this(
                System.getProperty("remote.logging.host", "127.0.0.1"),
                Integer.parseInt(System.getProperty("remote.logging.port", "9990")));
    }

    RemoteAccessUdpLogger(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    void logLoginSuccess(final String username, final String clientIp, final int clientPort) {
        send("LOGIN_SUCCESS", username, clientIp, clientPort);
    }

    void logLoginFailure(final String username, final String clientIp, final int clientPort) {
        send("LOGIN_FAILURE", username, clientIp, clientPort);
    }

    void logLogout(final String username, final String clientIp, final int clientPort) {
        send("LOGOUT", username, clientIp, clientPort);
    }

    void logDisconnect(final String username, final String clientIp, final int clientPort) {
        send("DISCONNECT", username, clientIp, clientPort);
    }

    private void send(final String eventType,
                      final String username,
                      final String clientIp,
                      final int clientPort) {
        final String safeUser = username == null || username.isBlank() ? "-" : username;
        final String payload = eventType + '|'
                + Instant.now() + '|'
                + safeUser + '|'
                + clientIp + '|'
                + clientPort + '|'
                + SERVICE_ID;

        try (var socket = new DatagramSocket()) {
            final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            final InetAddress address = InetAddress.getByName(host);
            final DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (final IOException e) {
            System.err.println("Remote access log (UDP) failed: " + e.getMessage());
        }
    }
}
