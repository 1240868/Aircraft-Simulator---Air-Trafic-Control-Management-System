package eapli.aisafe.app.backoffice.console.presentation.fleet;

import eapli.aisafe.fleet.application.RegisterEngineModelController;
import eapli.aisafe.fleet.domain.EngineType;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterEngineModelUI extends AbstractUI {

    private final RegisterEngineModelController controller = new RegisterEngineModelController();

    @Override
    protected boolean doShow() {
        final int kind = Console.readInteger("Engine kind: 1 = Jet family (turboprop/turbofan/...), 2 = Electric propeller, 0 = cancel");
        if (kind == 0) {
            return false;
        }
        try {
            if (kind == 1) {
                registerJet();
            } else if (kind == 2) {
                registerElectric();
            } else {
                System.out.println("Invalid option.");
            }
        } catch (final IntegrityViolationException ex) {
            System.out.println("Could not register: " + ex.getMessage());
        } catch (final IllegalArgumentException ex) {
            System.out.println("Invalid value: " + ex.getMessage());
        }
        return false;
    }

    private void registerJet() {
        final String modelName = Console.readLine("Engine model name");
        final String manufacturer = Console.readLine("Manufacturer");
        final String typeStr = Console.readLine("Engine type (TURBOPROP, TURBOFAN, TURBOJET, RAMJET, OTHER — not ELECTRIC)");
        final EngineType type = EngineType.valueOf(typeStr.trim().toUpperCase());
        final double seaStatic = Console.readDouble("Sea-level static thrust (numeric value)");
        final String thrustUnit = Console.readLine("Thrust unit (e.g. kN)");
        final double cruiseThrust = Console.readDouble("Thrust at cruise (same unit)");
        final double tsfc = Console.readDouble("TSFC (positive)");
        final double density = Console.readDouble("Fuel density (e.g. kg/l for JET A-1)");
        final double energy = Console.readDouble("Specific energy (e.g. MJ/kg)");
        final var em = controller.registerJet(modelName, manufacturer, type, seaStatic, thrustUnit, cruiseThrust,
                tsfc, density, energy);
        System.out.println("Engine model registered: " + em.modelName() + " / " + em.manufacturer() + " id=" + em.identity());
    }

    private void registerElectric() {
        final String modelName = Console.readLine("Engine model name");
        final String manufacturer = Console.readLine("Manufacturer");
        final double power = Console.readDouble("Power (numeric value)");
        final String powerUnit = Console.readLine("Power unit (e.g. kW)");
        final double efficiency = Console.readDouble("Efficiency (positive, dimensionless)");
        final double density = Console.readDouble("Energy storage density (positive, e.g. kg/l equivalent)");
        final double energy = Console.readDouble("Specific energy (positive, e.g. MJ/kg equivalent)");
        final var em = controller.registerElectric(modelName, manufacturer, power, powerUnit, efficiency, density, energy);
        System.out.println("Engine model registered: " + em.modelName() + " / " + em.manufacturer() + " id=" + em.identity());
    }

    @Override
    public String headline() {
        return "Register engine model";
    }
}
