package eapli.aisafe.app.remote.logging.server;

import eapli.aisafe.app.remote.logging.common.RemoteAccessLogEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thread-safe in-memory store for received remote access events. */
public final class RemoteAccessLogStore {

    private static final int MAX_EVENTS = 500;

    private final Deque<RemoteAccessLogEntry> recentEvents = new ArrayDeque<>();
    private final Map<String, ActiveSession> activeSessions = new LinkedHashMap<>();

    public synchronized void add(final RemoteAccessLogEntry entry) {
        if (recentEvents.size() >= MAX_EVENTS) {
            recentEvents.removeFirst();
        }
        recentEvents.addLast(entry);
        applySessionRules(entry);
    }

    public synchronized List<RemoteAccessLogEntry> recentEvents() {
        return List.copyOf(recentEvents);
    }

    public synchronized List<ActiveSession> activeSessions() {
        return List.copyOf(activeSessions.values());
    }

    private void applySessionRules(final RemoteAccessLogEntry entry) {
        final String key = sessionKey(entry);

        if (entry.isLoginSuccess()) {
            activeSessions.put(key, new ActiveSession(
                    entry.username(),
                    entry.clientIp(),
                    entry.clientPort(),
                    entry.serviceId(),
                    entry.timestamp()));
            return;
        }

        if (entry.isLogoutOrDisconnect()) {
            activeSessions.remove(key);
        }
    }

    private static String sessionKey(final RemoteAccessLogEntry entry) {
        return entry.username() + '|' + entry.clientIp() + '|' + entry.clientPort() + '|' + entry.serviceId();
    }

    public record ActiveSession(String username,
                               String clientIp,
                               int clientPort,
                               String serviceId,
                               java.time.Instant since) {
    }
}
