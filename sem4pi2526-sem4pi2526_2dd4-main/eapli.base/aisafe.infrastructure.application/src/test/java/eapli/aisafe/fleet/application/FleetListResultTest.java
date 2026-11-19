package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** US072a–d — resultado de listagem de frota. */
public class FleetListResultTest {

    @Test
    public void ofShouldReturnNullMessageWhenRowsPresent() {
        final var row = new AircraftFleetListItemDTO("CS-TST", "A320", "Airbus", "ACTIVE", 180, null);
        final FleetListResult r = FleetListResult.of(List.of(row), FleetListResult.NO_RESULTS_BY_MODEL);
        assertEquals(1, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
    }

    @Test
    public void ofShouldSetEmptyMessageWhenNoRowsByModel() {
        final FleetListResult r = FleetListResult.of(List.of(), FleetListResult.NO_RESULTS_BY_MODEL);
        assertTrue(r.aircraft().isEmpty());
        assertEquals(FleetListResult.NO_RESULTS_BY_MODEL, r.messageWhenEmpty());
    }

    @Test
    public void ofShouldSetEmptyMessageWhenNoRowsByMaker() {
        final FleetListResult r = FleetListResult.of(List.of(), FleetListResult.NO_RESULTS_BY_MAKER);
        assertEquals(FleetListResult.NO_RESULTS_BY_MAKER, r.messageWhenEmpty());
    }

    @Test
    public void ofShouldSetEmptyMessageWhenNoRowsByCapacity() {
        final FleetListResult r = FleetListResult.of(List.of(), FleetListResult.NO_RESULTS_BY_CAPACITY);
        assertEquals(FleetListResult.NO_RESULTS_BY_CAPACITY, r.messageWhenEmpty());
    }

    @Test
    public void ofShouldSetEmptyMessageWhenNoRowsByAge() {
        final FleetListResult r = FleetListResult.of(List.of(), FleetListResult.NO_RESULTS_BY_AGE);
        assertEquals(FleetListResult.NO_RESULTS_BY_AGE, r.messageWhenEmpty());
    }

    @Test
    public void constructorShouldDefensivelyCopyList() {
        final List<AircraftFleetListItemDTO> backing = new ArrayList<>();
        backing.add(new AircraftFleetListItemDTO("CS-TST", "A320", "Airbus", "ACTIVE", 180, null));
        final FleetListResult r = FleetListResult.of(backing, FleetListResult.NO_RESULTS_BY_CAPACITY);
        backing.clear();
        assertEquals(1, r.aircraft().size());
    }

    @Test
    public void constructorShouldRejectNullList() {
        assertThrows(NullPointerException.class, () -> new FleetListResult(null, "msg"));
    }

    @Test
    public void canonicalConstructorShouldPreserveExplicitMessage() {
        final FleetListResult r = new FleetListResult(List.of(), "custom empty");
        assertEquals("custom empty", r.messageWhenEmpty());
    }

    @Test
    public void ofWithMultipleRowsShouldKeepAllRows() {
        final List<AircraftFleetListItemDTO> rows = List.of(
                new AircraftFleetListItemDTO("CS-A", "A320", "Airbus", "ACTIVE", 180, null),
                new AircraftFleetListItemDTO("CS-B", "A320", "Airbus", "ACTIVE", 160, null));
        final FleetListResult r = FleetListResult.of(rows, FleetListResult.NO_RESULTS_BY_MAKER);
        assertEquals(2, r.aircraft().size());
        assertNull(r.messageWhenEmpty());
    }
}
