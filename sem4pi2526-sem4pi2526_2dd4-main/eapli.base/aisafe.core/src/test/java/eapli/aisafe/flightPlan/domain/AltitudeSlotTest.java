package eapli.aisafe.flightPlan.domain;

import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AltitudeSlotTest {

    @Test
    void shouldCreateValidSlot() {
        final AltitudeSlot slot = new AltitudeSlot(10000.0, 40000.0, MeasurementUnit.METER);
        assertEquals(10000.0, slot.minAltitude());
        assertEquals(40000.0, slot.maxAltitude());
        assertEquals(MeasurementUnit.METER, slot.unit());
    }

    @Test
    void shouldAcceptEqualMinMax() {
        final AltitudeSlot slot = new AltitudeSlot(35000.0, 35000.0, MeasurementUnit.METER);
        assertEquals(35000.0, slot.minAltitude());
        assertEquals(35000.0, slot.maxAltitude());
    }

    @Test
    void shouldAcceptZero() {
        final AltitudeSlot slot = new AltitudeSlot(0.0, 0.0, MeasurementUnit.METER);
        assertEquals(0.0, slot.minAltitude());
        assertEquals(0.0, slot.maxAltitude());
    }

    @Test
    void shouldRejectMinExceedingMax() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(40000.0, 10000.0, MeasurementUnit.METER));
    }

    @Test
    void shouldRejectNegativeMin() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(-1.0, 10000.0, MeasurementUnit.METER));
    }

    @Test
    void shouldRejectNegativeMax() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(0.0, -1.0, MeasurementUnit.METER));
    }

    @Test
    void shouldRejectNullUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(10000.0, 40000.0, null));
    }

    @Test
    void shouldRejectNonMeterUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeSlot(10000.0, 40000.0, MeasurementUnit.KILOMETER));
    }
}
