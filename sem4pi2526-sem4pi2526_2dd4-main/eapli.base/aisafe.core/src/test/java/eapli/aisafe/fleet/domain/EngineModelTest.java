package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class EngineModelTest {

    private static FuelSpecs jetFuel() {
        return new FuelSpecs(FuelType.JET_A1, 0.8, 43e6);
    }

    private static JetEngineSpecs validJetSpecs() {
        return new JetEngineSpecs(
                new Thrust(100, "kN"),
                new Thrust(80, "kN"),
                0.04);
    }

    private static FuelSpecs electricFuel() {
        return new FuelSpecs(FuelType.ELECTRICITY, 250.0, 500.0);
    }

    private static ElectricEngineSpecs validElectricSpecs() {
        return new ElectricEngineSpecs(new Power(2.0, "MW"), 0.9);
    }

    private static EngineModel newViaPrivateConstructor(final String modelName,
                                                          final String manufacturer,
                                                          final EngineType type,
                                                          final FuelSpecs fuelSpecs,
                                                          final JetEngineSpecs jetSpecs,
                                                          final ElectricEngineSpecs electricSpecs)
            throws Exception {
        final Constructor<EngineModel> ctor = EngineModel.class.getDeclaredConstructor(
                String.class, String.class, EngineType.class,
                FuelSpecs.class, JetEngineSpecs.class, ElectricEngineSpecs.class);
        ctor.setAccessible(true);
        return ctor.newInstance(modelName, manufacturer, type, fuelSpecs, jetSpecs, electricSpecs);
    }

    @Test
    public void createJetShouldPersistTypeAndSpecs() {
        final var em = EngineModel.createJet("PW127", "Pratt", EngineType.TURBOPROP, jetFuel(), validJetSpecs());
        assertNotNull(em.identity());
        assertEquals("PW127", em.modelName());
        assertEquals("Pratt", em.manufacturer());
        assertEquals(EngineType.TURBOPROP, em.type());
        assertEquals(FuelType.JET_A1, em.fuelSpecs().type());
        assertNotNull(em.jetSpecs());
        assertNull(em.electricSpecs());
        assertEquals(100.0, em.jetSpecs().atSeaLevelStatic().value(), 0.001);
    }

    @Test
    public void createJetShouldRejectElectricPropellerType() {
        assertThrows(Exception.class, () -> EngineModel.createJet(
                "X", "Y", EngineType.ELECTRIC_PROPELLER, jetFuel(), validJetSpecs()));
    }

    @Test
    public void createJetShouldRejectNullSpecs() {
        assertThrows(NullPointerException.class, () -> EngineModel.createJet(
                "X", "Y", EngineType.TURBOFAN, jetFuel(), null));
    }

    @Test
    public void createJetShouldRejectElectricityFuel() {
        final var badFuel = new FuelSpecs(FuelType.ELECTRICITY, 1.0, 1.0);
        assertThrows(Exception.class, () -> EngineModel.createJet(
                "EJ", "Co", EngineType.TURBOFAN, badFuel, validJetSpecs()));
    }

    @Test
    public void createElectricShouldSetTypeAndSpecs() {
        final var em = EngineModel.createElectric("EP1", "Volts", electricFuel(), validElectricSpecs());
        assertEquals(EngineType.ELECTRIC_PROPELLER, em.type());
        assertNotNull(em.electricSpecs());
        assertEquals(2.0, em.electricSpecs().power().value(), 0.001);
        assertEquals(FuelType.ELECTRICITY, em.fuelSpecs().type());
        assertNull(em.jetSpecs());
    }

    @Test
    public void createElectricShouldRejectNullSpecs() {
        assertThrows(NullPointerException.class, () -> EngineModel.createElectric(
                "E", "Co", electricFuel(), null));
    }

    @Test
    public void createElectricShouldRejectJetFuel() {
        assertThrows(Exception.class, () -> EngineModel.createElectric(
                "E", "Co", jetFuel(), validElectricSpecs()));
    }

    @Test
    public void sameAsShouldHoldForSameInstance() {
        final var em = EngineModel.createJet("A", "B", EngineType.TURBOJET, jetFuel(), validJetSpecs());
        assertTrue(em.sameAs(em));
    }

    @Test
    public void sameAsShouldRejectDifferentAggregate() {
        final var a = EngineModel.createJet("A1", "C", EngineType.TURBOJET, jetFuel(), validJetSpecs());
        final var b = EngineModel.createJet("A2", "C", EngineType.TURBOJET, jetFuel(), validJetSpecs());
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsShouldRejectNonEngineModel() {
        assertFalse(EngineModel.createJet("A", "B", EngineType.RAMJET, jetFuel(), validJetSpecs()).sameAs("x"));
    }

    @Test
    public void isCompatibleWithShouldMatchType() {
        final var em = EngineModel.createJet("R", "Co", EngineType.RAMJET, jetFuel(), validJetSpecs());
        assertTrue(em.isCompatibleWith(EngineType.RAMJET));
        assertFalse(em.isCompatibleWith(EngineType.TURBOPROP));
    }

    @Test
    public void createJetShouldRejectInvalidJetSpecs() {
        assertThrows(IllegalArgumentException.class, () -> {
            final var badSpecs = new JetEngineSpecs(
                    new Thrust(50, "kN"),
                    new Thrust(80, "kN"),
                    0.04);
            EngineModel.createJet("J", "C", EngineType.TURBOFAN, jetFuel(), badSpecs);
        });
    }

    @Test
    public void createJetShouldRejectBlankModelName() {
        assertThrows(Exception.class, () -> EngineModel.createJet(
                "  ", "Co", EngineType.TURBOFAN, jetFuel(), validJetSpecs()));
    }

    @Test
    public void createJetShouldRejectBlankManufacturer() {
        assertThrows(Exception.class, () -> EngineModel.createJet(
                "PW127", "  ", EngineType.TURBOFAN, jetFuel(), validJetSpecs()));
    }

    @Test
    public void createJetShouldRejectNullFuelSpecs() {
        assertThrows(NullPointerException.class, () -> EngineModel.createJet(
                "PW127", "Pratt", EngineType.TURBOFAN, null, validJetSpecs()));
    }

    @Test
    public void ensureSpecsShouldRejectElectricWithoutElectricSpecs() throws Exception {
        assertThrows(Exception.class, () -> newViaPrivateConstructor(
                "EP", "Co", EngineType.ELECTRIC_PROPELLER, electricFuel(), null, null));
    }

    @Test
    public void ensureSpecsShouldRejectElectricWithJetSpecs() throws Exception {
        assertThrows(Exception.class, () -> newViaPrivateConstructor(
                "EP", "Co", EngineType.ELECTRIC_PROPELLER, electricFuel(),
                validJetSpecs(), validElectricSpecs()));
    }

    @Test
    public void ensureSpecsShouldRejectElectricWithWrongFuelType() throws Exception {
        assertThrows(Exception.class, () -> newViaPrivateConstructor(
                "EP", "Co", EngineType.ELECTRIC_PROPELLER, jetFuel(), null, validElectricSpecs()));
    }

    @Test
    public void ensureSpecsShouldRejectJetWithoutJetSpecs() throws Exception {
        assertThrows(Exception.class, () -> newViaPrivateConstructor(
                "J", "Co", EngineType.TURBOFAN, jetFuel(), null, null));
    }

    @Test
    public void ensureSpecsShouldRejectJetWithElectricSpecs() throws Exception {
        assertThrows(Exception.class, () -> newViaPrivateConstructor(
                "J", "Co", EngineType.TURBOFAN, jetFuel(), validJetSpecs(), validElectricSpecs()));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<EngineModel> ctor = EngineModel.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
