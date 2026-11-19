package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class CabinSeatBlockTest {

    @Test
    public void shouldCreateValidBlock() {
        final var block = new CabinSeatBlock("ECONOMY", 150);
        assertEquals("ECONOMY", block.cabinClass());
        assertEquals(150, block.seatCount());
    }

    @Test
    public void shouldTrimCabinClassName() {
        assertEquals("BUSINESS", new CabinSeatBlock("  BUSINESS  ", 20).cabinClass());
    }

    @Test
    public void shouldAcceptZeroSeats() {
        assertEquals(0, new CabinSeatBlock("FIRST", 0).seatCount());
    }

    @Test
    public void shouldRejectNullCabinClass() {
        assertThrows(IllegalArgumentException.class, () -> new CabinSeatBlock(null, 10));
    }

    @Test
    public void shouldRejectEmptyCabinClass() {
        assertThrows(IllegalArgumentException.class, () -> new CabinSeatBlock("", 10));
    }

    @Test
    public void shouldRejectNegativeSeatCount() {
        assertThrows(IllegalArgumentException.class, () -> new CabinSeatBlock("ECONOMY", -1));
    }

    @Test
    public void equalsSameValues() {
        assertEquals(new CabinSeatBlock("ECONOMY", 150), new CabinSeatBlock("ECONOMY", 150));
    }

    @Test
    public void equalsDifferentClass() {
        assertNotEquals(new CabinSeatBlock("ECONOMY", 150), new CabinSeatBlock("BUSINESS", 150));
    }

    @Test
    public void equalsDifferentCount() {
        assertNotEquals(new CabinSeatBlock("ECONOMY", 150), new CabinSeatBlock("ECONOMY", 100));
    }

    @Test
    public void equalsSameInstance() {
        final var a = new CabinSeatBlock("ECONOMY", 150);
        assertEquals(a, a);
    }

    @Test
    public void equalsNull() {
        assertFalse(new CabinSeatBlock("ECONOMY", 150).equals(null));
    }

    @Test
    public void equalsDifferentType() {
        assertFalse(new CabinSeatBlock("ECONOMY", 150).equals("ECONOMY"));
    }

    @Test
    public void hashCodeEqual() {
        assertEquals(new CabinSeatBlock("ECONOMY", 150).hashCode(), new CabinSeatBlock("ECONOMY", 150).hashCode());
    }

    @Test
    public void hashCodeDifferent() {
        assertNotEquals(new CabinSeatBlock("ECONOMY", 150).hashCode(), new CabinSeatBlock("BUSINESS", 20).hashCode());
    }

    @Test
    public void protectedConstructorShouldExist() throws Exception {
        final Constructor<CabinSeatBlock> ctor = CabinSeatBlock.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
