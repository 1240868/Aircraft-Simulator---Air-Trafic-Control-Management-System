package eapli.aisafe.app.remote.logging.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Emits UDP datagrams to the Remote Accesses Logging Server.
 * Payload: eventType|timestamp|username|clientIp|clientPort|serviceId
 */
public final class RemoteAccessUdpLogger {

    private final String host;
    private final int port;
    private final RemoteAccessServiceId serviceId;

    public RemoteAccessUdpLogger(final RemoteAccessServiceId serviceId) {
        this(RemoteAccessLoggingConfig.host(), RemoteAccessLoggingConfig.port(), serviceId);
    }

    public RemoteAccessUdpLogger(final String host, final int port, final RemoteAccessServiceId serviceId) {
        this.host = host;
        this.port = port;
        this.serviceId = serviceId;
    }

    public void logLoginSuccess(final String username, final String clientIp, final int clientPort) {
        send("LOGIN_SUCCESS", username, clientIp, clientPort);
    }

    public void logLoginFailure(final String username, final String clientIp, final int clientPort) {
        send("LOGIN_FAILURE", username, clientIp, clientPort);
    }

    public void logLogout(final String username, final String clientIp, final int clientPort) {
        send("LOGOUT", username, clientIp, clientPort);
    }

    public void logDisconnect(final String username, final String clientIp, final int clientPort) {
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
                + serviceId.code();

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
