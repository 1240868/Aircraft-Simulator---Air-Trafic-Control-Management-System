package eapli.aisafe.app.pilot.client;

import java.io.IOException;
import java.util.Scanner;

/** TCP client for Pilot remote access (Air Transport Company App). */
public final class PilotClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8890;

    public static void main(final String[] args) {
        try (var conn = new ServerConnection(HOST, PORT);
             var scanner = new Scanner(System.in)) {

            System.out.println("=== Pilot Client ===");
            System.out.println("Please authenticate to access pilot functionalities.");

            boolean running = true;
            while (running) {
                if (!promptLogin(conn, scanner)) {
                    break;
                }
                running = runAuthenticatedMenu(conn, scanner);
            }
        } catch (final IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static boolean promptLogin(final ServerConnection conn, final Scanner scanner) throws IOException {
        while (true) {
            System.out.println("\n--- Authentication ---");
            System.out.println("1  Login");
            System.out.println("2  Ping server");
            System.out.println("0  Exit");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> {
                    System.out.print("Username: ");
                    final String username = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    final String password = scanner.nextLine();
                    final String loginResp = conn.send("LOGIN|" + username + "|" + password);
                    if ("OK".equals(loginResp)) {
                        System.out.println("Login successful. Welcome, " + username + ".");
                        return true;
                    }
                    System.out.println("Login failed: " + loginResp);
                }
                case "2" -> {
                    System.out.println(conn.send("PING"));
                }
                case "0" -> {
                    return false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static boolean runAuthenticatedMenu(final ServerConnection conn, final Scanner scanner)
            throws IOException {
        boolean sessionActive = true;
        while (sessionActive) {
            System.out.println("\n=== Pilot Client - authenticated ===");
            System.out.println("1  Register flight plan");
            System.out.println("2  Register flight plan from DSL file");
            System.out.println("3  Insert weather data in flight plan");
            System.out.println("4  Test / validate flight plan");
            System.out.println("5  List my flight plans");
            System.out.println("6  List testable flight plans");
            System.out.println("7  Consult weather data");
            System.out.println("8  List flight routes (helper)");
            System.out.println("9  List aircraft (helper)");
            System.out.println("0  Logout");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> handleRegisterFlightPlan(conn, scanner);
                case "2" -> handleRegisterFromFile(conn, scanner);
                case "3" -> handleInsertWeather(conn, scanner);
                case "4" -> handleTestFlightPlan(conn, scanner);
                case "5" -> handleListMyPlans(conn);
                case "6" -> handleListTestablePlans(conn);
                case "7" -> handleConsultWeather(conn, scanner);
                case "8" -> handleListRoutes(conn);
                case "9" -> handleListAircraft(conn);
                case "0" -> {
                    final String logoutResp = conn.send("LOGOUT");
                    System.out.println(logoutResp);
                    conn.reconnect();
                    sessionActive = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
        return true;
    }

    private static void handleRegisterFlightPlan(final ServerConnection conn,
                                                 final Scanner scanner) throws IOException {
        System.out.print("Flight route id (UUID): ");
        final String routeId = scanner.nextLine().trim();
        System.out.print("Aircraft registration: ");
        final String aircraftReg = scanner.nextLine().trim();
        System.out.print("Flight designator (e.g. TP1920): ");
        final String designator = scanner.nextLine().trim();
        System.out.print("Flight type (COMMERCIAL, CARGO, CHARTER, PRIVATE, MILITARY, EMERGENCY): ");
        final String flightType = scanner.nextLine().trim();
        System.out.print("Departure date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();
        System.out.print("Departure time (HH:mm): ");
        final String time = scanner.nextLine().trim();

        final String cmd = "REGISTER_FLIGHT_PLAN|" + routeId + '|' + aircraftReg + '|'
                + designator + '|' + flightType + '|' + date + '|' + time;
        final String resp = conn.send(cmd);
        if (resp.startsWith("OK|")) {
            System.out.println("Flight plan registered: " + resp.substring(3));
        } else {
            System.out.println(resp);
        }
    }

    private static void handleRegisterFromFile(final ServerConnection conn,
                                               final Scanner scanner) throws IOException {
        System.out.print("Absolute path to DSL file on server machine: ");
        final String path = scanner.nextLine().trim();
        System.out.print("Flight route id (UUID): ");
        final String routeId = scanner.nextLine().trim();
        System.out.print("Aircraft registration: ");
        final String aircraftReg = scanner.nextLine().trim();

        final String resp = conn.send("REGISTER_FLIGHT_PLAN_FILE|" + path + '|' + routeId + '|' + aircraftReg);
        if (resp.startsWith("OK|")) {
            System.out.println("Flight plan registered from file: " + resp.substring(3));
        } else if (resp.startsWith("ERROR|DSL_VALIDATION_FAILED|")) {
            System.out.println("DSL validation failed:");
            System.out.println(resp.substring("ERROR|DSL_VALIDATION_FAILED|".length()).replace("  ", "\n"));
        } else {
            System.out.println(resp);
        }
    }

    private static void handleInsertWeather(final ServerConnection conn,
                                            final Scanner scanner) throws IOException {
        System.out.print("Flight plan id (UUID): ");
        final String planId = scanner.nextLine().trim();
        System.out.print("Leg id (UUID): ");
        final String legId = scanner.nextLine().trim();
        System.out.print("Area code (e.g. PT-LIS): ");
        final String area = scanner.nextLine().trim();
        System.out.print("Observed at (ISO-8601 UTC): ");
        final String observedAt = scanner.nextLine().trim();
        System.out.print("Temperature (C): ");
        final String temp = scanner.nextLine().trim();
        System.out.print("Humidity (%): ");
        final String humidity = scanner.nextLine().trim();
        System.out.print("Wind speed (m/s): ");
        final String windSpeed = scanner.nextLine().trim();
        System.out.print("Wind direction (degrees): ");
        final String windDir = scanner.nextLine().trim();

        final String cmd = "INSERT_WEATHER|" + planId + '|' + legId + '|' + area + '|'
                + observedAt + '|' + temp + '|' + humidity + '|' + windSpeed + '|' + windDir;
        final String resp = conn.send(cmd);
        if ("OK".equals(resp)) {
            System.out.println("Weather data inserted. Any previous test is void.");
        } else {
            System.out.println(resp);
        }
    }

    private static void handleTestFlightPlan(final ServerConnection conn,
                                             final Scanner scanner) throws IOException {
        System.out.print("Flight plan id (UUID) to test: ");
        final String planId = scanner.nextLine().trim();
        final String resp = conn.send("TEST_FLIGHT_PLAN|" + planId);

        if (resp.startsWith("OK|PASSED|")) {
            System.out.println("Flight plan PASSED validation. Status: " + resp.substring(10));
        } else if (resp.startsWith("OK|FAILED|")) {
            System.out.println("Flight plan FAILED validation:");
            for (final String line : resp.split("\n")) {
                if (line.startsWith("DSL|")) {
                    System.out.println("  DSL: " + line.substring(4));
                } else if (line.startsWith("PHYSICS|")) {
                    System.out.println("  Physics: " + line.substring(8));
                } else if (line.startsWith("OK|FAILED|")) {
                    System.out.println("  Status: " + line.substring(10));
                }
            }
        } else {
            System.out.println(resp);
        }
    }

    private static void handleListMyPlans(final ServerConnection conn) throws IOException {
        printListResponse(conn.send("LIST_MY_FLIGHT_PLANS"), "My flight plans");
    }

    private static void handleListTestablePlans(final ServerConnection conn) throws IOException {
        printListResponse(conn.send("LIST_TESTABLE_PLANS"), "Testable flight plans");
    }

    private static void handleConsultWeather(final ServerConnection conn,
                                             final Scanner scanner) throws IOException {
        System.out.print("Area code: ");
        final String area = scanner.nextLine().trim();
        System.out.print("Date (YYYY-MM-DD): ");
        final String date = scanner.nextLine().trim();
        printListResponse(conn.send("CONSULT_WEATHER|" + area + '|' + date), "Weather observations");
    }

    private static void handleListRoutes(final ServerConnection conn) throws IOException {
        printListResponse(conn.send("LIST_ROUTES"), "Flight routes");
    }

    private static void handleListAircraft(final ServerConnection conn) throws IOException {
        printListResponse(conn.send("LIST_AIRCRAFT"), "Aircraft");
    }

    private static void printListResponse(final String resp, final String title) {
        if (!resp.startsWith("OK|")) {
            System.out.println(resp);
            return;
        }
        final String[] lines = resp.split("\n");
        final int count = Integer.parseInt(lines[0].substring(3));
        System.out.println(title + " (" + count + "):");
        for (int i = 1; i < lines.length; i++) {
            System.out.println("  " + lines[i]);
        }
    }

    private PilotClient() {
    }
}
