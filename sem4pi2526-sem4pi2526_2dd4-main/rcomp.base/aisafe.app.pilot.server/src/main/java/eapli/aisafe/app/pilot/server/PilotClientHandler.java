package eapli.aisafe.app.pilot.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

final class PilotClientHandler implements Runnable {

    private final Socket socket;
    private final PilotSessionHandler sessionHandler;

    PilotClientHandler(final Socket socket) {
        this.socket = socket;
        this.sessionHandler = new PilotSessionHandler(
                socket.getInetAddress().getHostAddress(),
                socket.getPort(),
                new RemoteAccessUdpLogger());
    }

    @Override
    public void run() {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                final String response = sessionHandler.processCommand(line.trim());
                out.print(response);
                out.print('\n');
                out.flush();
                if (response.startsWith("BYE")) {
                    break;
                }
            }
        } catch (final IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            sessionHandler.notifyDisconnect();
            try {
                socket.close();
            } catch (final IOException ignored) {
            }
        }
    }
}
