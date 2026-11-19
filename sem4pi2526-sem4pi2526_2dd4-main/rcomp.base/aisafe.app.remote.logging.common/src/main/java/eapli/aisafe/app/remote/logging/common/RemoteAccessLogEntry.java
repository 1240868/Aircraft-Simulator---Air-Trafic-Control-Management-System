package eapli.aisafe.app.remote.logging.common;

import java.time.Instant;
import java.util.Objects;

/** Parsed remote access event received over UDP. */
public final class RemoteAccessLogEntry {

    private final String eventType;
    private final Instant timestamp;
    private final String username;
    private final String clientIp;
    private final int clientPort;
    private final String serviceId;

    public RemoteAccessLogEntry(final String eventType,
                                final Instant timestamp,
                                final String username,
                                final String clientIp,
                                final int clientPort,
                                final String serviceId) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.username = username;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.serviceId = serviceId;
    }

    public String eventType() {
        return eventType;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String username() {
        return username;
    }

    public String clientIp() {
        return clientIp;
    }

    public int clientPort() {
        return clientPort;
    }

    public String serviceId() {
        return serviceId;
    }

    public boolean isLoginSuccess() {
        return "LOGIN_SUCCESS".equals(eventType);
    }

    public boolean isLogoutOrDisconnect() {
        return "LOGOUT".equals(eventType) || "DISCONNECT".equals(eventType);
    }

    @Override
    public String toString() {
        return eventType + '|' + timestamp + '|' + username + '|' + clientIp + '|' + clientPort + '|' + serviceId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RemoteAccessLogEntry that)) {
            return false;
        }
        return clientPort == that.clientPort
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(username, that.username)
                && Objects.equals(clientIp, that.clientIp)
                && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, timestamp, username, clientIp, clientPort, serviceId);
    }
}
