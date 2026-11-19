package eapli.aisafe.app.remote.logging.common;

/** Tracks authenticated username for logout/disconnect logging within a TCP session. */
public final class RemoteAccessSessionTracker {

    private String loggedInUsername;
    private boolean disconnectLogged;

    public void onLoginSuccess(final String username) {
        this.loggedInUsername = username;
        this.disconnectLogged = false;
    }

    public void onLogout() {
        this.disconnectLogged = true;
        this.loggedInUsername = null;
    }

    public String loggedInUsername() {
        return loggedInUsername;
    }

    public boolean shouldLogDisconnect() {
        return loggedInUsername != null && !disconnectLogged;
    }

    public void markDisconnectLogged() {
        this.disconnectLogged = true;
    }
}
