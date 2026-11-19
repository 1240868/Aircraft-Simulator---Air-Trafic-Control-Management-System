package app;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java app.Main <flight-plan-file>");
            System.exit(1);
        }

        Path file = Path.of(args[0]);
        FlightPlanImporter importer = new FlightPlanImporter();
        ImportResult result = importer.importFromFile(file);

        if (result.isValid()) {
            var fp = result.flightPlan();
            System.out.println("Flight plan imported successfully.");
            System.out.println("  Flight ID  : " + fp.flight().id());
            System.out.println("  Type       : " + fp.flight().type());
            System.out.println("  Legs       : " + fp.flight().legs().size());
            if (fp.flight().route() != null) {
                System.out.println("  Route      : " + fp.flight().route().id());
            }
            System.exit(0);
        }

        System.err.println("Flight plan import failed.");
        if (!result.syntaxErrors().isEmpty()) {
            System.err.println("Syntax errors:");
            result.syntaxErrors().forEach(e -> System.err.println("  " + e));
        }
        if (!result.semanticErrors().isEmpty()) {
            System.err.println("Semantic errors:");
            result.semanticErrors().forEach(e -> System.err.println("  " + e));
        }
        System.exit(2);
    }
}