package eapli.aisafe.app.atc.server;

import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.app.remote.logging.common.RemoteAccessCommandObserver;
import eapli.aisafe.app.remote.logging.common.RemoteAccessLoggingConfig;
import eapli.aisafe.app.remote.logging.common.RemoteAccessServiceId;
import eapli.aisafe.app.remote.logging.common.RemoteAccessSessionTracker;
import eapli.aisafe.app.remote.logging.common.RemoteAccessUdpLogger;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.application.dto.PilotRosterListItemDTO;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.roles.Roles;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

/**
 * TCP client handler for ATCC remote access with external logging.
 * Mirrors {@link ClientHandler} behaviour and adds UDP event emission.
 */
public final class AtcLoggingClientHandler implements Runnable {

    private final Socket socket;
    private final RemoteAccessUdpLogger accessLogger;
    private final RemoteAccessSessionTracker sessionTracker = new RemoteAccessSessionTracker();
    private UserSession currentSession;

    public AtcLoggingClientHandler(final Socket socket) {
        this(socket, new RemoteAccessUdpLogger(RemoteAccessServiceId.ATC));
    }

    AtcLoggingClientHandler(final Socket socket, final RemoteAccessUdpLogger accessLogger) {
        this.socket = socket;
        this.accessLogger = accessLogger;
    }

    @Override
    public void run() {
        final String clientIp = socket.getInetAddress().getHostAddress();
        final int clientPort = socket.getPort();

        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                final String command = line.trim();
                final String response = processCommand(command);
                RemoteAccessCommandObserver.observe(
                        command, response, clientIp, clientPort, accessLogger, sessionTracker);
                out.println(response);
                if (response.startsWith("BYE")) {
                    break;
                }
            }
        } catch (final IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            RemoteAccessCommandObserver.observeDisconnect(clientIp, clientPort, accessLogger, sessionTracker);
            try {
                socket.close();
            } catch (final IOException ignored) {
            }
        }
    }

    private String processCommand(final String line) {
        final String[] parts = line.split("\\|", -1);
        final String cmd = parts[0].toUpperCase();

        return switch (cmd) {
            case "LOGIN" -> handleLogin(parts);
            case "LOGOUT" -> handleLogout();
            case "PING" -> "PONG";
            case "LIST_PILOTS" -> handleListPilots();
            default -> "ERROR|UNKNOWN_COMMAND";
        };
    }

    private String handleLogin(final String[] parts) {
        if (parts.length < 3) {
            return "ERROR|INVALID_FORMAT";
        }

        final String username = parts[1];
        final String password = parts[2];

        try {
            final Constructor<AuthorizationService> ctor =
                    AuthorizationService.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            final AuthorizationService localAuthz = ctor.newInstance();

            final AuthenticationService localAuthn = new AuthenticationService(
                    PersistenceContext.repositories().users(),
                    localAuthz,
                    new AisafePasswordPolicy(),
                    new PlainTextEncoder());

            final Optional<UserSession> session =
                    localAuthn.authenticate(username, password, Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

            if (session.isEmpty()) {
                return "ERROR|INVALID_CREDENTIALS";
            }

            this.currentSession = session.get();
            return "OK";

        } catch (final Exception e) {
            return "ERROR|AUTH_FAILED";
        }
    }

    private String handleLogout() {
        this.currentSession = null;
        return "BYE";
    }

    private String handleListPilots() {
        if (currentSession == null) {
            return "ERROR|NOT_AUTHENTICATED";
        }

        final SystemUser user = currentSession.authenticatedUser();
        if (!user.hasAny(Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            return "ERROR|FORBIDDEN";
        }

        final AuthorizationPort authz = new AuthorizationPort() {
            @Override
            public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
                if (!user.hasAny(roles)) {
                    throw new SecurityException("User not authorized");
                }
            }

            @Override
            public String currentUsername() {
                return user.username().toString();
            }
        };

        final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();
        final AirTransportCompanyRepository companyRepo =
                PersistenceContext.repositories().airTransportCompanies();
        final var modelRepo = PersistenceContext.repositories().aircraftModels();

        final var controller = new ListPilotRosterController(authz, pilotRepo, companyRepo, modelRepo);
        final var result = controller.listPilotRoster();
        final List<PilotRosterListItemDTO> pilots = result.pilots();

        if (pilots.isEmpty()) {
            return "OK|0";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("OK|").append(pilots.size());
        for (final PilotRosterListItemDTO p : pilots) {
            sb.append('\n').append(p.pilotId())
                    .append('|').append(p.fullName())
                    .append('|').append(p.email())
                    .append('|').append(p.certification())
                    .append('|').append(p.licenseNumber())
                    .append('|').append(p.licenseExpiration())
                    .append('|').append(p.status())
                    .append('|').append(String.join(";", p.certifiedModels()));
        }
        return sb.toString();
    }

    /** Loads remote logging settings before starting the ATC TCP server with external logging enabled. */
    public static void bootstrapRemoteLoggingConfig() {
        RemoteAccessLoggingConfig.loadClasspathProperties("application.properties");
        RemoteAccessLoggingConfig.loadClasspathProperties("remote-logging.properties");
    }
}
