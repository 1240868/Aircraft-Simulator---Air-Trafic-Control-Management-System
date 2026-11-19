package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class AircraftTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private static final AircraftModelId MODEL = AircraftModelId.newId();
    private static final LocalDate DEFAULT_MFG = LocalDate.of(2015, 3, 10);

    private Aircraft validAircraft() {
        return AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 150), new CabinSeatBlock("BUSINESS", 12)),
                180, DEFAULT_MFG, null);
    }

    @Test
    public void shouldCreateActiveAircraft() {
        assertEquals(AircraftOperationalStatus.ACTIVE, validAircraft().operationalStatus());
    }

    @Test
    public void identityShouldReturnRegistration() {
        assertEquals("CS-TTK", validAircraft().identity().toString());
    }

    @Test
    public void ownerCompanyIdShouldMatch() {
        assertEquals(COMPANY, validAircraft().ownerCompanyId());
    }

    @Test
    public void aircraftModelIdShouldMatch() {
        assertEquals(MODEL, validAircraft().aircraftModelId());
    }

    @Test
    public void registrationCountryShouldMatch() {
        assertEquals("PT", validAircraft().registrationCountry().toString());
    }

    @Test
    public void cabinSeatBlocksShouldReturnCorrectSize() {
        assertEquals(2, validAircraft().cabinSeatBlocks().size());
    }

    @Test
    public void cabinSeatBlocksShouldBeUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> validAircraft().cabinSeatBlocks().add(new CabinSeatBlock("FIRST", 5)));
    }

    @Test
    public void totalConfiguredSeatsShouldSumAllBlocks() {
        assertEquals(162, validAircraft().totalConfiguredSeats());
    }

    @Test
    public void totalConfiguredSeatsWithSingleBlock() {
        final var a = AircraftFactory.registerNew("AB-CDE", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, DEFAULT_MFG, null);
        assertEquals(100, a.totalConfiguredSeats());
    }

    @Test
    public void isRetiredShouldBeFalseInitially() {
        assertFalse(validAircraft().isRetired());
    }

    @Test
    public void markRetiredShouldChangeStatus() {
        final var a = validAircraft();
        a.markRetired();
        assertTrue(a.isRetired());
        assertEquals(AircraftOperationalStatus.RETIRED, a.operationalStatus());
    }

    @Test
    public void markRetiredTwiceShouldFail() {
        final var a = validAircraft();
        a.markRetired();
        assertThrows(IllegalStateException.class, a::markRetired);
    }

    @Test
    public void sameAsSelfShouldReturnTrue() {
        final var a = validAircraft();
        assertTrue(a.sameAs(a));
    }

    @Test
    public void sameAsNullShouldReturnFalse() {
        assertFalse(validAircraft().sameAs(null));
    }

    @Test
    public void sameAsDifferentTypeShouldReturnFalse() {
        assertFalse(validAircraft().sameAs("not aircraft"));
    }

    @Test
    public void sameAsDifferentRegistrationShouldReturnFalse() {
        final var a = validAircraft();
        final var b = AircraftFactory.registerNew("CS-TTX", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, DEFAULT_MFG, null);
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsSameRegistrationShouldReturnTrue() {
        final var a = AircraftFactory.registerNew("CS-AAA", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, DEFAULT_MFG, null);
        final var b = AircraftFactory.registerNew("CS-AAA", "PT", AircraftModelId.newId(),
                new CompanyId(UUID.randomUUID()),
                List.of(new CabinSeatBlock("BUSINESS", 50)), 50, DEFAULT_MFG, null);
        assertTrue(a.sameAs(b));
    }

    @Test
    public void constructorShouldRejectNullRegistration() {
        assertThrows(NullPointerException.class, () -> new Aircraft(
                null, AircraftRegistrationCountry.valueOf("PT"), MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), DEFAULT_MFG, null, AircraftOperationalStatus.ACTIVE));
    }

    @Test
    public void constructorShouldRejectNullCountry() {
        assertThrows(NullPointerException.class, () -> new Aircraft(
                RegistrationNumber.valueOf("CS-TTK"), null, MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), DEFAULT_MFG, null, AircraftOperationalStatus.ACTIVE));
    }

    @Test
    public void constructorShouldRejectNullModel() {
        assertThrows(NullPointerException.class, () -> new Aircraft(
                RegistrationNumber.valueOf("CS-TTK"), AircraftRegistrationCountry.valueOf("PT"), null, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), DEFAULT_MFG, null, AircraftOperationalStatus.ACTIVE));
    }

    @Test
    public void constructorShouldRejectNullCompany() {
        assertThrows(NullPointerException.class, () -> new Aircraft(
                RegistrationNumber.valueOf("CS-TTK"), AircraftRegistrationCountry.valueOf("PT"), MODEL, null,
                List.of(new CabinSeatBlock("ECONOMY", 10)), DEFAULT_MFG, null, AircraftOperationalStatus.ACTIVE));
    }

    @Test
    public void constructorShouldRejectNullStatus() {
        assertThrows(NullPointerException.class, () -> new Aircraft(
                RegistrationNumber.valueOf("CS-TTK"), AircraftRegistrationCountry.valueOf("PT"), MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), DEFAULT_MFG, null, null));
    }

    @Test
    public void isListedForActiveFleetByModelShouldBeTrueWhenMatchAndActive() {
        final var a = validAircraft();
        assertTrue(a.isListedForActiveFleetByModel(COMPANY, MODEL));
    }

    @Test
    public void isListedForActiveFleetByModelShouldBeFalseWhenWrongCompany() {
        final var a = validAircraft();
        assertFalse(a.isListedForActiveFleetByModel(new CompanyId(UUID.randomUUID()), MODEL));
    }

    @Test
    public void isListedForActiveFleetByModelShouldBeFalseWhenWrongModel() {
        final var a = validAircraft();
        assertFalse(a.isListedForActiveFleetByModel(COMPANY, AircraftModelId.newId()));
    }

    @Test
    public void isListedForActiveFleetByModelShouldBeFalseWhenRetired() {
        final var a = validAircraft();
        a.markRetired();
        assertFalse(a.isListedForActiveFleetByModel(COMPANY, MODEL));
    }

    @Test
    public void isListedForActiveFleetByModelShouldBeTrueWhenMaintenance() {
        final var a = new Aircraft(
                RegistrationNumber.valueOf("CS-MNT"),
                AircraftRegistrationCountry.valueOf("PT"),
                MODEL,
                COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 50)),
                LocalDate.of(2016, 1, 1),
                null,
                AircraftOperationalStatus.MAINTENANCE);
        assertTrue(a.isListedForActiveFleetByModel(COMPANY, MODEL));
    }

    @Test
    public void isListedForActiveFleetByPassengerCapacityShouldBeTrueWhenMatchAndActive() {
        final var a = validAircraft();
        assertTrue(a.isListedForActiveFleetByPassengerCapacity(COMPANY, 162));
    }

    @Test
    public void isListedForActiveFleetByPassengerCapacityShouldBeFalseWhenWrongCompany() {
        final var a = validAircraft();
        assertFalse(a.isListedForActiveFleetByPassengerCapacity(new CompanyId(UUID.randomUUID()), 162));
    }

    @Test
    public void isListedForActiveFleetByPassengerCapacityShouldBeFalseWhenWrongSeatCount() {
        final var a = validAircraft();
        assertFalse(a.isListedForActiveFleetByPassengerCapacity(COMPANY, 100));
    }

    @Test
    public void isListedForActiveFleetByPassengerCapacityShouldBeFalseWhenRetired() {
        final var a = validAircraft();
        a.markRetired();
        assertFalse(a.isListedForActiveFleetByPassengerCapacity(COMPANY, 162));
    }

    @Test
    public void isListedForActiveFleetByPassengerCapacityShouldBeTrueWhenMaintenance() {
        final var a = new Aircraft(
                RegistrationNumber.valueOf("CS-MNT"),
                AircraftRegistrationCountry.valueOf("PT"),
                MODEL,
                COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 88)),
                LocalDate.of(2016, 1, 1),
                null,
                AircraftOperationalStatus.MAINTENANCE);
        assertTrue(a.isListedForActiveFleetByPassengerCapacity(COMPANY, 88));
    }

    @Test
    public void ageReferenceDateShouldPreferManufacturingOverFirstRegistration() {
        final var a = AircraftFactory.registerNew("CS-D1", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 100,
                LocalDate.of(2010, 1, 1), LocalDate.of(2011, 6, 1));
        assertEquals(LocalDate.of(2010, 1, 1), a.ageReferenceDate().orElseThrow());
    }

    @Test
    public void ageReferenceDateShouldUseFirstRegistrationWhenManufacturingMissing() {
        final var a = AircraftFactory.registerNew("CS-D2", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 100,
                null, LocalDate.of(2012, 6, 15));
        assertEquals(LocalDate.of(2012, 6, 15), a.ageReferenceDate().orElseThrow());
    }

    @Test
    public void ageInCompleteYearsShouldUseChronoUnitYears() {
        final var a = AircraftFactory.registerNew("CS-D3", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 100,
                LocalDate.of(2020, 6, 15), null);
        assertEquals(4, a.ageInCompleteYears(LocalDate.of(2025, 6, 14)));
        assertEquals(5, a.ageInCompleteYears(LocalDate.of(2025, 6, 15)));
        assertEquals(6, a.ageInCompleteYears(LocalDate.of(2026, 6, 15)));
    }

    @Test
    public void ageInCompleteYearsShouldFailWhenNoReferenceDate() {
        final var a = new Aircraft(
                RegistrationNumber.valueOf("CS-ND"),
                AircraftRegistrationCountry.valueOf("PT"),
                MODEL,
                COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)),
                null,
                null,
                AircraftOperationalStatus.ACTIVE);
        assertThrows(IllegalStateException.class, () -> a.ageInCompleteYears(LocalDate.now()));
    }

    @Test
    public void isListedForActiveFleetByAgeShouldMatchExactYears() {
        final var a = AircraftFactory.registerNew("CS-AG", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 100,
                LocalDate.of(2019, 1, 1), null);
        assertTrue(a.isListedForActiveFleetByAge(COMPANY, 5, LocalDate.of(2024, 1, 1)));
        assertFalse(a.isListedForActiveFleetByAge(COMPANY, 4, LocalDate.of(2024, 1, 1)));
    }

    @Test
    public void isListedForActiveFleetByAgeShouldBeFalseWithoutReferenceDates() {
        final var a = new Aircraft(
                RegistrationNumber.valueOf("CS-XX"),
                AircraftRegistrationCountry.valueOf("PT"),
                MODEL,
                COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)),
                null,
                null,
                AircraftOperationalStatus.ACTIVE);
        assertFalse(a.isListedForActiveFleetByAge(COMPANY, 5, LocalDate.of(2024, 1, 1)));
    }

    @Test
    public void isListedForActiveFleetByAgeShouldExcludeRetired() {
        final var a = AircraftFactory.registerNew("CS-RT", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 10)), 100,
                LocalDate.of(2018, 1, 1), null);
        a.markRetired();
        assertFalse(a.isListedForActiveFleetByAge(COMPANY, 8, LocalDate.of(2026, 1, 1)));
    }

    @Test
    public void protectedConstructorShouldExist() throws Exception {
        final Constructor<Aircraft> ctor = Aircraft.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
