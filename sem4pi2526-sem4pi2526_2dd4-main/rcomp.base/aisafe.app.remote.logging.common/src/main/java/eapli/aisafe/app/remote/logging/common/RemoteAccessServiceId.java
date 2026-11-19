package eapli.aisafe.app.remote.logging.common;

/** Identifies the remote TCP service that originated an access event. */
public enum RemoteAccessServiceId {
    WEATHER("WEATHER"),
    ATC("ATC"),
    PILOT("PILOT");

    private final String code;

    RemoteAccessServiceId(final String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
