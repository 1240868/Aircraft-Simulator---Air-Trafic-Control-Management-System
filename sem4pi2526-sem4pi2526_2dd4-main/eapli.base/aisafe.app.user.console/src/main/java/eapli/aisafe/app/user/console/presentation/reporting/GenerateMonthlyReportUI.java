package eapli.aisafe.app.user.console.presentation.reporting;

import eapli.aisafe.app.user.console.presentation.AbstractBaseUI;
import eapli.aisafe.reporting.application.GenerateMonthlyReportController;
import eapli.aisafe.reporting.application.MonthlyReportResult;
import eapli.framework.io.util.Console;

/** US112 — generate a monthly statistics report. Actor: Flight Control Operator. */
public class GenerateMonthlyReportUI extends AbstractBaseUI {

    private final GenerateMonthlyReportController controller =
            new GenerateMonthlyReportController();

    @Override
    protected boolean doShow() {
        System.out.println("Generate Monthly Statistics Report");
        System.out.println("Report will cover the full calendar month for your air control area.");
        System.out.println();

        final int year = Console.readInteger("Year (e.g. 2026): ");
        final int month = Console.readInteger("Month (1-12): ");

        if (month < 1 || month > 12) {
            System.out.println("Invalid month. Must be between 1 and 12.");
            return false;
        }

        System.out.println();
        System.out.println("Generating report — please wait...");

        try {
            final MonthlyReportResult result = controller.generate(year, month);
            if (result.success()) {
                System.out.println("Report generated successfully.");
                System.out.println("Saved to: " + result.reportPath().toAbsolutePath());
            } else {
                System.out.println("ERROR: " + result.errorMessage());
            }
        } catch (final IllegalStateException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return super.headline() + " > Generate monthly report";
    }
}
