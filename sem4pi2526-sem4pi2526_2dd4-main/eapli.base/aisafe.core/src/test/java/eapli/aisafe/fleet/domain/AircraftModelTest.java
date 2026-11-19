package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class AircraftModelTest {

    private static List<EngineModelId> oneEngine() {
        return List.of(EngineModelId.newId());
    }

    private static AircraftModel validModel() {
        return new AircraftModel(
                "A320neo",
                "Airbus",
                AircraftType.PASSENGER,
                EngineType.TURBOFAN,
                2,
                180,
                6000.0,
                oneEngine());
    }

    @Test
    public void shouldCreateWithValidData() {
        final var m = validModel();
        assertNotNull(m.identity());
        assertEquals("A320neo", m.modelName());
        assertEquals("Airbus", m.manufacturer());
        assertEquals(AircraftType.PASSENGER, m.type());
        assertEquals(EngineType.TURBOFAN, m.motorization());
        assertEquals(2, m.numberOfEngines());
        assertEquals(180, m.passengerCapacity());
        assertEquals(6000.0, m.maximumRangeKm(), 0.001);
        assertEquals(1, m.certifiedEngineModelIds().size());
    }

    @Test
    public void certifiedEngineModelIdsShouldMatchInput() {
        final var e1 = EngineModelId.newId();
        final var e2 = EngineModelId.newId();
        final var m = new AircraftModel(
                "B737",
                "Boeing",
                AircraftType.CARGO,
                EngineType.TURBOFAN,
                2,
                2,
                5000.0,
                List.of(e1, e2));
        final var ids = m.certifiedEngineModelIds();
        assertEquals(e1.asUuid(), ids.get(0).asUuid());
        assertEquals(e2.asUuid(), ids.get(1).asUuid());
    }

    @Test
    public void certifiedEngineModelIdsViewShouldBeUnmodifiable() {
        final var m = validModel();
        assertThrows(UnsupportedOperationException.class,
                () -> m.certifiedEngineModelIds().add(EngineModelId.newId()));
    }

    @Test
    public void sameAsShouldHoldForSameInstance() {
        final var m = validModel();
        assertTrue(m.sameAs(m));
    }

    @Test
    public void sameAsShouldRejectDifferentAggregate() {
        final var a = validModel();
        final var b = validModel();
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsShouldRejectNonAircraftModel() {
        assertFalse(validModel().sameAs("x"));
    }

    @Test
    public void shouldRejectBlankModelName() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "  ", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 1, 10, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectBlankManufacturer() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "  ", AircraftType.MIXED, EngineType.TURBOPROP, 1, 10, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectNullAircraftType() {
        assertThrows(NullPointerException.class, () -> new AircraftModel(
                "M1", "Mfr", null, EngineType.TURBOPROP, 1, 10, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectNullMotorization() {
        assertThrows(NullPointerException.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, null, 1, 10, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectNonPositiveEngines() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 0, 10, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectNonPositivePassengerCapacity() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 1, 0, 100.0, oneEngine()));
    }

    @Test
    public void shouldRejectNonPositiveRange() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 1, 10, 0.0, oneEngine()));
    }

    @Test
    public void shouldRejectNullCertifiedEnginesList() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 1, 10, 100.0, null));
    }

    @Test
    public void shouldRejectEmptyCertifiedEnginesList() {
        assertThrows(Exception.class, () -> new AircraftModel(
                "M1", "Mfr", AircraftType.MIXED, EngineType.TURBOPROP, 1, 10, 100.0, List.of()));
    }

    @Test
    public void protectedConstructorShouldExistForOrm() throws Exception {
        final Constructor<AircraftModel> ctor = AircraftModel.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }

    @Test
    public void shouldCopyCertifiedEnginesDefensively() {
        final var list = new ArrayList<EngineModelId>();
        list.add(EngineModelId.newId());
        final var m = new AircraftModel(
                "M1", "Mfr", AircraftType.PASSENGER, EngineType.TURBOFAN, 1, 50, 2000.0, list);
        list.clear();
        assertEquals(1, m.certifiedEngineModelIds().size());
    }
}
