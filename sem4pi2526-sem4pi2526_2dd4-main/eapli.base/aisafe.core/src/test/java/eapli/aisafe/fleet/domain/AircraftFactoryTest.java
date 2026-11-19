package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import eapli.aisafe.company.domain.CompanyId;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class AircraftFactoryTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private static final AircraftModelId MODEL = AircraftModelId.newId();
    private static final LocalDate MFG = LocalDate.of(2018, 6, 1);

    @Test
    public void shouldCreateValidAircraft() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null);
        assertNotNull(a);
        assertEquals("CS-TTK", a.identity().toString());
        assertEquals(AircraftOperationalStatus.ACTIVE, a.operationalStatus());
    }

    @Test
    public void shouldCreateWithMultipleCabinBlocks() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 150), new CabinSeatBlock("BUSINESS", 30)), 180, MFG, null);
        assertEquals(180, a.totalConfiguredSeats());
    }

    @Test
    public void shouldAllowFewerSeatsThanCapacity() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 50)), 180, MFG, null);
        assertEquals(50, a.totalConfiguredSeats());
    }

    @Test
    public void shouldRejectZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), 0, MFG, null));
    }

    @Test
    public void shouldRejectNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), -1, MFG, null));
    }

    @Test
    public void shouldRejectNullCabinBreakdown() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, null, 100, MFG, null));
    }

    @Test
    public void shouldRejectEmptyCabinBreakdown() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, Collections.emptyList(), 100, MFG, null));
    }

    @Test
    public void shouldRejectSeatsExceedingCapacity() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 200)), 100, MFG, null));
    }

    @Test
    public void shouldRejectZeroTotalSeats() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 0)), 100, MFG, null));
    }

    @Test
    public void shouldRejectInvalidRegistration() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "@@@", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null));
    }

    @Test
    public void shouldRejectInvalidCountry() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PORTUGAL", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null));
    }

    @Test
    public void shouldSetCorrectCountry() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null);
        assertEquals("PT", a.registrationCountry().toString());
    }

    @Test
    public void shouldSetCorrectModel() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null);
        assertEquals(MODEL, a.aircraftModelId());
    }

    @Test
    public void registerWithOperationalStatusShouldApplyStatus() {
        final var a = AircraftFactory.registerWithOperationalStatus(
                "CS-MNT", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 50)), 180, MFG, null,
                AircraftOperationalStatus.MAINTENANCE);
        assertEquals(AircraftOperationalStatus.MAINTENANCE, a.operationalStatus());
    }

    @Test
    public void shouldSetCorrectCompany() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, MFG, null);
        assertEquals(COMPANY, a.ownerCompanyId());
    }

    @Test
    public void shouldRejectWhenBothDatesMissing() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), 100, null, null));
    }

    @Test
    public void shouldAcceptFirstRegistrationOnly() {
        final var a = AircraftFactory.registerNew("CS-TTK", "PT", MODEL, COMPANY,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 100, null, LocalDate.of(2019, 1, 1));
        assertEquals(LocalDate.of(2019, 1, 1), a.firstRegistrationDate().orElseThrow());
    }

    @Test
    public void shouldRejectFutureManufacturingDate() {
        assertThrows(IllegalArgumentException.class, () -> AircraftFactory.registerNew(
                "CS-TTK", "PT", MODEL, COMPANY, List.of(new CabinSeatBlock("ECONOMY", 100)), 100,
                LocalDate.now().plusDays(1), null));
    }
}
