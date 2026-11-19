package eapli.aisafe.app.pilot.server;

import eapli.aisafe.infrastructure.bootstrappers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** TCP server for Pilot remote access. */
public final class PilotServer {

    private static final int PORT = 8890;

    public static void main(final String[] args) {
        AuthzRegistry.configure(
                PersistenceContext.repositories().users(),
                new AisafePasswordPolicy(),
                new PlainTextEncoder());

        new AisafeBootstrapper().execute();

        System.out.println("Pilot Server listening on port " + PORT);

        try (var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new Thread(new PilotClientHandler(clientSocket)).start();
            }
        } catch (final IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private PilotServer() {
    }
}
