package eapli.aisafe.app.pilot.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** TCP connection to the Pilot remote access server. */
final class ServerConnection implements Closeable {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    ServerConnection(final String host, final int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    void reconnect() throws IOException {
        closeQuietly();
        connect();
    }

    private void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    String send(final String command) throws IOException {
        out.println(command);
        final String firstLine = in.readLine();
        if (firstLine == null) {
            throw new IOException("Connection closed by server");
        }

        if (!firstLine.startsWith("OK|") || command.startsWith("LOGIN")) {
            return firstLine;
        }

        if (firstLine.startsWith("OK|FAILED|") || firstLine.startsWith("OK|PASSED|")) {
            return readUntilEnd(firstLine);
        }

        if (firstLine.startsWith("ERROR|DSL_VALIDATION_FAILED|")) {
            return readUntilEnd(firstLine);
        }

        int count;
        try {
            count = Integer.parseInt(firstLine.substring(3));
        } catch (final NumberFormatException e) {
            return firstLine;
        }

        final StringBuilder response = new StringBuilder(firstLine);
        int linesRead = 0;
        while (linesRead < count) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            response.append('\n').append(line);
            if (!line.startsWith("LEG|")) {
                linesRead++;
            }
        }
        return response.toString();
    }

    private String readUntilEnd(final String header) throws IOException {
        final StringBuilder response = new StringBuilder(header);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                break;
            }
            response.append('\n').append(line);
            if (!line.startsWith("DSL|") && !line.startsWith("PHYSICS|") && !line.startsWith("LEG|")) {
                break;
            }
        }
        return response.toString();
    }

    private void closeQuietly() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (final IOException ignored) {
        }
    }

    @Override
    public void close() throws IOException {
        closeQuietly();
    }
}
