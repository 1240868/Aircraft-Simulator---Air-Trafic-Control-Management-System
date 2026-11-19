package eapli.aisafe.app.user.console.presentation.simulation;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import eapli.aisafe.simulation.application.SimulationReportResult;
import eapli.framework.io.util.Console;

/** US111 — generate a simulation report. Actor: Flight Control Operator. */
public class GenerateSimulationReportUI extends AbstractBaseUI {

    private final GenerateSimulationReportController controller =
            new GenerateSimulationReportController();

    @Override
    protected boolean doShow() {
        System.out.println("This will run the flight simulation and generate a summary report.");
        System.out.println("Ensure the C executable is compiled: cd scomp.base && make compile");
        System.out.println();

        final String confirm = Console.readLine("Proceed with simulation? (yes/no): ");
        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println("Simulation cancelled.");
            return false;
        }

        System.out.println();
        System.out.println("Running simulation — please wait...");
        System.out.println("-".repeat(65));

        final SimulationReportResult result = controller.generateReport();

        System.out.println("-".repeat(65));
        System.out.println();

        if (!result.success()) {
            System.out.println("ERROR: " + result.errorMessage());
            return false;
        }

        System.out.println("Simulation completed successfully.");
        System.out.println("Report saved to: " + result.reportPath().toAbsolutePath());
        System.out.println();
        System.out.println(result.reportContent());

        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Generate simulation report";
    }
}
