package eapli.aisafe.app.remote.logging.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** TCP listener that accepts browser connections and spawns an HTTP handler per request. */
public final class RemoteAccessHttpServer implements Runnable {

    private final int port;
    private final RemoteAccessLogStore store;

    public RemoteAccessHttpServer(final int port, final RemoteAccessLogStore store) {
        this.port = port;
        this.store = store;
    }

    @Override
    public void run() {
        try (var serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP status server listening on port " + port
                    + "  ->  open http://localhost:" + port + "/ in your browser");
            while (!Thread.currentThread().isInterrupted()) {
                final Socket client = serverSocket.accept();
                final Thread t = new Thread(new RemoteAccessHttpHandler(client, store));
                t.setDaemon(true);
                t.start();
            }
        } catch (final IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("HTTP server error: " + e.getMessage());
            }
        }
    }
}
