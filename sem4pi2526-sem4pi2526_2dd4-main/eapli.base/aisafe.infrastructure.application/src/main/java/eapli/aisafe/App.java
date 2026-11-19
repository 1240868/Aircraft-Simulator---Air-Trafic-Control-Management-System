package eapli.aisafe;

public class App {
    public static final String VERSION = "1.0.0";
    public static final String COPYRIGHT = "(C) 2026, AISafe Team";

    private static final Settings SETTINGS = new Settings();

    public static Settings settings() {
        return SETTINGS;
    }

    private App() {
    }
}
