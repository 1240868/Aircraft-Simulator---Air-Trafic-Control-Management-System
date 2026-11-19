package eapli.aisafe.app.atc.client;

import java.io.IOException;
import java.util.Scanner;

public final class ATCClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8888;

    public static void main(final String[] args) {
        try (var conn = new ServerConnection(HOST, PORT);
             var scanner = new Scanner(System.in)) {

            String username = null;
            boolean running = true;

            while (running) {
                System.out.println("\n=== ATC Client ===");
                System.out.println("1  Login");
                System.out.println("2  Ping");
                System.out.println("3  List Pilots");
                System.out.println("4  Logout");
                System.out.println("0  Exit");
                System.out.print("Option: ");

                final String option = scanner.nextLine().trim();

                switch (option) {
                    case "1":
                        System.out.print("Username: ");
                        username = scanner.nextLine().trim();
                        System.out.print("Password: ");
                        final String pwd = scanner.nextLine();
                        final String loginResp = conn.send("LOGIN|" + username + "|" + pwd);
                        System.out.println(loginResp);
                        break;

                    case "2":
                        final String pingResp = conn.send("PING");
                        System.out.println(pingResp);
                        break;

                    case "3":
                        if (username == null) {
                            System.out.println("Please login first.");
                        } else {
                            final String pilotsResp = conn.send("LIST_PILOTS");
                            if (pilotsResp.startsWith("OK")) {
                                final String[] lines = pilotsResp.split("\n");
                                System.out.println("Pilots (" + lines[0].substring(3) + "):");
                                for (int i = 1; i < lines.length; i++) {
                                    final String[] fields = lines[i].split("\\|");
                                    if (fields.length >= 2) {
                                        System.out.println("  " + fields[1] + " (" + fields[0] + ")");
                                    }
                                }
                            } else {
                                System.out.println(pilotsResp);
                            }
                        }
                        break;

                    case "4":
                        final String logoutResp = conn.send("LOGOUT");
                        System.out.println(logoutResp);
                        username = null;
                        break;

                    case "0":
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (final IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private ATCClient() {
    }
}
