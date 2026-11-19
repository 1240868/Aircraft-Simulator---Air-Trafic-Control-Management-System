package eapli.aisafe.app.remote.logging.common;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Parses UDP datagram payloads for remote access events. */
public final class RemoteAccessDatagramParser {

    private RemoteAccessDatagramParser() {
    }

    public static RemoteAccessLogEntry parse(final String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Empty datagram payload");
        }

        final String[] parts = payload.trim().split("\\|", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid datagram format: " + payload);
        }

        final String eventType = parts[0].trim();
        final Instant timestamp = parseTimestamp(parts[1]);
        final String username = parts[2].trim();
        final String clientIp = parts[3].trim();
        final int clientPort = parsePort(parts[4]);
        final String serviceId = parts[5].trim();

        return new RemoteAccessLogEntry(eventType, timestamp, username, clientIp, clientPort, serviceId);
    }

    private static Instant parseTimestamp(final String raw) {
        try {
            return Instant.parse(raw.trim());
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + raw, e);
        }
    }

    private static int parsePort(final String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid client port: " + raw, e);
        }
    }
}
