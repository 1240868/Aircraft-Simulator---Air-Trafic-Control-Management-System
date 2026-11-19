package eapli.aisafe.app.remote.logging.server;

import eapli.aisafe.app.remote.logging.common.RemoteAccessLogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles a single HTTP request from a browser.
 *
 * Routes:
 *   GET /                  -> index.html (static)
 *   GET /events            -> events.html (static, AJAX page)
 *   GET /active-users      -> active-users.html (static, AJAX page)
 *   GET /api/events        -> dynamic HTML fragment (latest events table)
 *   GET /api/active-users  -> dynamic HTML fragment (active sessions table)
 */
public final class RemoteAccessHttpHandler implements Runnable {

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Socket client;
    private final RemoteAccessLogStore store;

    public RemoteAccessHttpHandler(final Socket client, final RemoteAccessLogStore store) {
        this.client = client;
        this.store = store;
    }

    @Override
    public void run() {
        try (client) {
            final var in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            final OutputStream out = client.getOutputStream();

            final String requestLine = in.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            final String[] parts = requestLine.split(" ", 3);
            if (parts.length < 2) {
                return;
            }

            if (!"GET".equalsIgnoreCase(parts[0])) {
                send(out, 405, "Method Not Allowed", "text/plain; charset=utf-8", "Method Not Allowed");
                return;
            }

            final String rawPath = parts[1];
            final String path = rawPath.contains("?")
                    ? rawPath.substring(0, rawPath.indexOf('?'))
                    : rawPath;

            route(path, out);

        } catch (final IOException ignored) {
        }
    }

    private void route(final String path, final OutputStream out) throws IOException {
        switch (path) {
            case "/", "/index.html" ->
                    serveStaticFile("www/index.html", "text/html; charset=utf-8", out);
            case "/events", "/events.html" ->
                    serveStaticFile("www/events.html", "text/html; charset=utf-8", out);
            case "/active-users", "/active-users.html" ->
                    serveStaticFile("www/active-users.html", "text/html; charset=utf-8", out);
            case "/api/events" ->
                    serveEventsFragment(out);
            case "/api/active-users" ->
                    serveActiveUsersFragment(out);
            default ->
                    send(out, 404, "Not Found", "text/plain; charset=utf-8", "404 Not Found");
        }
    }

    private void serveStaticFile(final String resource, final String contentType,
                                  final OutputStream out) throws IOException {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            send(out, 404, "Not Found", "text/plain; charset=utf-8", "404 Not Found");
            return;
        }
        try (in) {
            sendBytes(out, 200, "OK", contentType, in.readAllBytes());
        }
    }

    private void serveEventsFragment(final OutputStream out) throws IOException {
        final List<RemoteAccessLogEntry> events = new ArrayList<>(store.recentEvents());
        Collections.reverse(events);

        final String html;
        if (events.isEmpty()) {
            html = "<p>No events recorded yet.</p>";
        } else {
            final var sb = new StringBuilder();
            sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" "
                    + "style=\"border-collapse:collapse;font-family:monospace\">")
              .append("<tr style=\"background:#cce\">")
              .append("<th>Time</th><th>Event</th><th>User</th>")
              .append("<th>Client IP</th><th>Port</th><th>Service</th>")
              .append("</tr>");
            for (final RemoteAccessLogEntry e : events) {
                sb.append("<tr>")
                  .append("<td>").append(DISPLAY.format(e.timestamp())).append("</td>")
                  .append("<td>").append(esc(e.eventType())).append("</td>")
                  .append("<td>").append(esc(e.username())).append("</td>")
                  .append("<td>").append(esc(e.clientIp())).append("</td>")
                  .append("<td>").append(e.clientPort()).append("</td>")
                  .append("<td>").append(esc(e.serviceId())).append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
            html = sb.toString();
        }
        send(out, 200, "OK", "text/html; charset=utf-8", html);
    }

    private void serveActiveUsersFragment(final OutputStream out) throws IOException {
        final List<RemoteAccessLogStore.ActiveSession> sessions = store.activeSessions();

        final String html;
        if (sessions.isEmpty()) {
            html = "<p>No users currently connected.</p>";
        } else {
            final var sb = new StringBuilder();
            sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" "
                    + "style=\"border-collapse:collapse;font-family:monospace\">")
              .append("<tr style=\"background:#cec\">")
              .append("<th>User</th><th>Client IP</th><th>Port</th><th>Service</th><th>Connected since</th>")
              .append("</tr>");
            for (final RemoteAccessLogStore.ActiveSession s : sessions) {
                sb.append("<tr>")
                  .append("<td>").append(esc(s.username())).append("</td>")
                  .append("<td>").append(esc(s.clientIp())).append("</td>")
                  .append("<td>").append(s.clientPort()).append("</td>")
                  .append("<td>").append(esc(s.serviceId())).append("</td>")
                  .append("<td>").append(DISPLAY.format(s.since())).append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
            html = sb.toString();
        }
        send(out, 200, "OK", "text/html; charset=utf-8", html);
    }

    private void send(final OutputStream out, final int status, final String statusText,
                      final String contentType, final String body) throws IOException {
        sendBytes(out, status, statusText, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private void sendBytes(final OutputStream out, final int status, final String statusText,
                           final String contentType, final byte[] body) throws IOException {
        final String headers = "HTTP/1.1 " + status + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static String esc(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
