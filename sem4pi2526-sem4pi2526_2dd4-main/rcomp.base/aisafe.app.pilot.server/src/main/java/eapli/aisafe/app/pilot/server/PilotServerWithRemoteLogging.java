package eapli.aisafe.app.pilot.server;

import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP server entry point for pilot remote access with external logging enabled.
 * Uses {@link PilotLoggingClientHandler} instead of the legacy {@link PilotClientHandler}.
 */
public final class PilotServerWithRemoteLogging {

    private static final int PORT = 8890;

    public static void main(final String[] args) {
        PilotLoggingClientHandler.bootstrapRemoteLoggingConfig();

        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(),
                new PlainTextEncoder());

        new AisafeBootstrapper().execute();

        System.out.println("Pilot Server (remote logging enabled) listening on port " + PORT);

        try (var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new Thread(new PilotLoggingClientHandler(clientSocket)).start();
            }
        } catch (final IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private PilotServerWithRemoteLogging() {
    }
}
