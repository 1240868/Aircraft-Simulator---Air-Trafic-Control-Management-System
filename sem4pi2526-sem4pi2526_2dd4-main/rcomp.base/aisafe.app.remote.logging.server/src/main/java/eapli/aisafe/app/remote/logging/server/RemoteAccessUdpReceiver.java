package eapli.aisafe.app.remote.logging.server;

import eapli.aisafe.app.remote.logging.common.RemoteAccessDatagramParser;
import eapli.aisafe.app.remote.logging.common.RemoteAccessLogEntry;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Receives UDP datagrams from remote access TCP servers. */
public final class RemoteAccessUdpReceiver implements Runnable {

    private final int listenPort;
    private final RemoteAccessLogStore store;
    private final Path logFile;

    public RemoteAccessUdpReceiver(final int listenPort,
                                   final RemoteAccessLogStore store,
                                   final Path logFile) {
        this.listenPort = listenPort;
        this.store = store;
        this.logFile = logFile;
    }

    @Override
    public void run() {
        try (var socket = new DatagramSocket(listenPort)) {
            System.out.println("Remote Accesses Logging Server listening on UDP port " + listenPort);
            final byte[] buffer = new byte[2048];

            while (!Thread.currentThread().isInterrupted()) {
                final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (final IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("UDP receiver error: " + e.getMessage());
            }
        }
    }

    private void handlePacket(final DatagramPacket packet) {
        final String payload = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                StandardCharsets.UTF_8);

        try {
            final RemoteAccessLogEntry entry = RemoteAccessDatagramParser.parse(payload);
            store.add(entry);
            appendToFile(entry);
            System.out.println("Logged: " + entry);
        } catch (final IllegalArgumentException e) {
            System.err.println("Ignored invalid datagram: " + payload + " (" + e.getMessage() + ")");
        }
    }

    private void appendToFile(final RemoteAccessLogEntry entry) {
        if (logFile == null) {
            return;
        }
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile, entry + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (final IOException e) {
            System.err.println("Could not append log file: " + e.getMessage());
        }
    }
}
