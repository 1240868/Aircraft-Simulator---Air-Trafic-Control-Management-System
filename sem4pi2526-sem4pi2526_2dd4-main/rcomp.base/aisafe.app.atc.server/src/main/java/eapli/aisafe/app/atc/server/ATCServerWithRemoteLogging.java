package eapli.aisafe.app.atc.server;

import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP server entry point for ATCC remote access with external logging enabled.
 * Uses {@link AtcLoggingClientHandler} instead of the legacy {@link ClientHandler}.
 */
public final class ATCServerWithRemoteLogging {

    private static final int PORT = 8888;

    public static void main(final String[] args) {
        AtcLoggingClientHandler.bootstrapRemoteLoggingConfig();

        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(),
                new PlainTextEncoder());

        new AisafeBootstrapper().execute();

        System.out.println("ATC Server (remote logging enabled) listening on port " + PORT);

        try (var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new Thread(new AtcLoggingClientHandler(clientSocket)).start();
            }
        } catch (final IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private ATCServerWithRemoteLogging() {
    }
}
