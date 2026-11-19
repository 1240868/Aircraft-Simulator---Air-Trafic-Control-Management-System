package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** US072 — linha de listagem de frota. */
public class AircraftFleetListItemDTOTest {

    @Test
    public void shouldExposeAllFieldsWhenAgePresent() {
        final AircraftFleetListItemDTO dto = new AircraftFleetListItemDTO(
                "CS-TST", "A320", "Airbus", "ACTIVE", 180, 5);

        assertEquals("CS-TST", dto.registrationNumber());
        assertEquals("A320", dto.modelName());
        assertEquals("Airbus", dto.manufacturer());
        assertEquals("ACTIVE", dto.operationalStatus());
        assertEquals(180, dto.numberOfSeats());
        assertEquals(Integer.valueOf(5), dto.ageInCompleteYears());
    }

    @Test
    public void shouldAllowNullAgeForNonAgeListings() {
        final AircraftFleetListItemDTO dto = new AircraftFleetListItemDTO(
                "CS-TST", "A320", "Airbus", "MAINTENANCE", 120, null);

        assertNull(dto.ageInCompleteYears());
        assertEquals("MAINTENANCE", dto.operationalStatus());
    }

    @Test
    public void equalsShouldDependOnAllComponents() {
        final AircraftFleetListItemDTO a = new AircraftFleetListItemDTO(
                "CS-A", "M1", "Mfr", "ACTIVE", 100, null);
        final AircraftFleetListItemDTO same = new AircraftFleetListItemDTO(
                "CS-A", "M1", "Mfr", "ACTIVE", 100, null);
        final AircraftFleetListItemDTO different = new AircraftFleetListItemDTO(
                "CS-B", "M1", "Mfr", "ACTIVE", 100, null);

        assertEquals(a, same);
        assertNotEquals(a, different);
    }

    @Test
    public void hashCodeShouldBeConsistentWithEquals() {
        final AircraftFleetListItemDTO a = new AircraftFleetListItemDTO(
                "CS-A", "M1", "Mfr", "ACTIVE", 100, 3);
        final AircraftFleetListItemDTO b = new AircraftFleetListItemDTO(
                "CS-A", "M1", "Mfr", "ACTIVE", 100, 3);

        assertEquals(a.hashCode(), b.hashCode());
    }
}
