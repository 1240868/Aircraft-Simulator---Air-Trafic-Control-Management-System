package eapli.aisafe.app.remote.logging.common;

/**
 * Observes TCP command/response pairs and triggers UDP logging for login, logout and failures.
 * Used by remote access servers without modifying existing session handlers.
 */
public final class RemoteAccessCommandObserver {

    private RemoteAccessCommandObserver() {
    }

    public static void observe(final String commandLine,
                               final String response,
                               final String clientIp,
                               final int clientPort,
                               final RemoteAccessUdpLogger logger,
                               final RemoteAccessSessionTracker tracker) {
        if (commandLine == null || commandLine.isBlank()) {
            return;
        }

        final String[] parts = commandLine.trim().split("\\|", -1);
        final String cmd = parts[0].toUpperCase();

        if ("LOGIN".equals(cmd)) {
            observeLogin(parts, response, clientIp, clientPort, logger, tracker);
            return;
        }

        if ("LOGOUT".equals(cmd)) {
            observeLogout(clientIp, clientPort, logger, tracker);
        }
    }

    public static void observeDisconnect(final String clientIp,
                                         final int clientPort,
                                         final RemoteAccessUdpLogger logger,
                                         final RemoteAccessSessionTracker tracker) {
        if (!tracker.shouldLogDisconnect()) {
            return;
        }
        logger.logDisconnect(tracker.loggedInUsername(), clientIp, clientPort);
        tracker.markDisconnectLogged();
    }

    private static void observeLogin(final String[] parts,
                                     final String response,
                                     final String clientIp,
                                     final int clientPort,
                                     final RemoteAccessUdpLogger logger,
                                     final RemoteAccessSessionTracker tracker) {
        final String username = parts.length > 1 ? parts[1] : "-";

        if ("OK".equals(response)) {
            tracker.onLoginSuccess(username);
            logger.logLoginSuccess(username, clientIp, clientPort);
            return;
        }

        if (response != null && response.startsWith("ERROR|")) {
            logger.logLoginFailure(username, clientIp, clientPort);
        }
    }

    private static void observeLogout(final String clientIp,
                                      final int clientPort,
                                      final RemoteAccessUdpLogger logger,
                                      final RemoteAccessSessionTracker tracker) {
        if (tracker.loggedInUsername() != null) {
            logger.logLogout(tracker.loggedInUsername(), clientIp, clientPort);
        }
        tracker.onLogout();
    }
}
