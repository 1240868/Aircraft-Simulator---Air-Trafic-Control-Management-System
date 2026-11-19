package eapli.aisafe.app.atc.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

final class ServerConnection implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    ServerConnection(final String host, final int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    String send(final String command) throws IOException {
        out.println(command);
        final StringBuilder response = new StringBuilder();
        String line = in.readLine();
        if (line == null) {
            throw new IOException("Connection closed by server");
        }
        response.append(line);

        if (line.startsWith("OK|") && !command.equals("LOGIN")) {
            int count = 0;
            try {
                final String countStr = line.substring(3);
                count = Integer.parseInt(countStr);
            } catch (final NumberFormatException e) {
                return response.toString();
            }
            for (int i = 0; i < count; i++) {
                line = in.readLine();
                if (line == null) break;
                response.append('\n').append(line);
            }
        }

        return response.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
