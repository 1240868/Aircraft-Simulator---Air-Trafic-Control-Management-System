package eapli.aisafe.fleet.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestAircraftRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestCompanyRepo;
import eapli.aisafe.fleet.application.FleetListingTestDoubles.TestModelRepo;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftFactory;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.AircraftOperationalStatus;
import eapli.aisafe.fleet.domain.AircraftType;
import eapli.aisafe.fleet.domain.CabinSeatBlock;
import eapli.aisafe.fleet.domain.EngineModelId;
import eapli.aisafe.fleet.domain.EngineType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

/** US072 — helpers partilhados pelos controladores de listagem. */
public class FleetListingTest {

    private static final LocalDate MFG = LocalDate.of(2018, 6, 1);

    @Test
    public void requireCompanyOfUserShouldReturnIdWhenLinked() {
        final var companies = new TestCompanyRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        companies.linkedCompany = Optional.of(cid);

        assertEquals(cid, FleetListing.requireCompanyOfUser(companies, "collab"));
    }

    @Test
    public void requireCompanyOfUserShouldFailWhenNotLinked() {
        final var companies = new TestCompanyRepo();

        assertThrows(IllegalStateException.class,
                () -> FleetListing.requireCompanyOfUser(companies, "orphan"));
    }

    @Test
    public void requireModelShouldReturnModelWhenPresent() {
        final var models = new TestModelRepo();
        final AircraftModel model = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
        models.save(model);

        assertEquals(model, FleetListing.requireModel(models, model.identity()));
    }

    @Test
    public void requireModelShouldFailWhenMissing() {
        final var models = new TestModelRepo();

        assertThrows(IllegalArgumentException.class,
                () -> FleetListing.requireModel(models, AircraftModelId.newId()));
    }

    @Test
    public void requireModelForAircraftShouldFailWhenModelMissing() {
        final var models = new TestModelRepo();
        final Aircraft aircraft = AircraftFactory.registerNew(
                "CS-TST", "PT", AircraftModelId.newId(), new CompanyId(UUID.randomUUID()),
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null);

        assertThrows(IllegalStateException.class,
                () -> FleetListing.requireModelForAircraft(models, aircraft));
    }

    @Test
    public void toRowShouldMapAircraftAndModelFields() {
        final AircraftModel model = new AircraftModel(
                "A320neo", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
        final Aircraft aircraft = AircraftFactory.registerNew(
                "CS-ROW", "PT", model.identity(), new CompanyId(UUID.randomUUID()),
                List.of(new CabinSeatBlock("ECONOMY", 150)), 180, MFG, null);

        final AircraftFleetListItemDTO row = FleetListing.toRow(aircraft, model);

        assertEquals("CS-ROW", row.registrationNumber());
        assertEquals("A320neo", row.modelName());
        assertEquals("Airbus", row.manufacturer());
        assertEquals("ACTIVE", row.operationalStatus());
        assertEquals(150, row.numberOfSeats());
        assertEquals(null, row.ageInCompleteYears());
    }

    @Test
    public void toRowWithAgeShouldIncludeCompleteYears() {
        final AircraftModel model = new AircraftModel(
                "E190", "Embraer", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 100, 3000, List.of(EngineModelId.newId()));
        final Aircraft aircraft = AircraftFactory.registerNew(
                "CS-AGE", "PT", model.identity(), new CompanyId(UUID.randomUUID()),
                List.of(new CabinSeatBlock("ECONOMY", 90)), 100, LocalDate.of(2020, 1, 1), null);
        final LocalDate asOf = LocalDate.of(2026, 5, 5);

        final AircraftFleetListItemDTO row = FleetListing.toRowWithAge(aircraft, model, asOf);

        assertEquals(Integer.valueOf(6), row.ageInCompleteYears());
    }

    @Test
    public void isKnownManufacturerShouldBeTrueWhenCatalogContainsMaker() {
        final var models = new TestModelRepo();
        models.save(new AircraftModel(
                "737", "Boeing", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId())));

        assertTrue(FleetListing.isKnownManufacturer(models, "boeing"));
    }

    @Test
    public void isKnownManufacturerShouldBeFalseWhenUnknown() {
        final var models = new TestModelRepo();
        models.save(new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId())));

        assertFalse(FleetListing.isKnownManufacturer(models, "unknown"));
    }

    @Test
    public void activeByManufacturerShouldExcludeRetiredAndOtherMakers() {
        final var models = new TestModelRepo();
        final var aircraftRepo = new TestAircraftRepo();
        final CompanyId cid = new CompanyId(UUID.randomUUID());
        final AircraftModel airbus = new AircraftModel(
                "A320", "Airbus", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
        final AircraftModel boeing = new AircraftModel(
                "737", "Boeing", AircraftType.PASSENGER, EngineType.TURBOFAN,
                2, 180, 6000, List.of(EngineModelId.newId()));
        models.save(airbus);
        models.save(boeing);

        aircraftRepo.byCompany.add(AircraftFactory.registerNew(
                "CS-A1", "PT", airbus.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null));
        aircraftRepo.byCompany.add(AircraftFactory.registerNew(
                "CS-B1", "PT", boeing.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 100)), 180, MFG, null));
        aircraftRepo.byCompany.add(AircraftFactory.registerWithOperationalStatus(
                "CS-A2", "PT", airbus.identity(), cid,
                List.of(new CabinSeatBlock("ECONOMY", 50)), 180, MFG, null,
                AircraftOperationalStatus.RETIRED));

        final List<Aircraft> active = FleetListing.activeByManufacturer(
                aircraftRepo, models, cid, "airbus");

        assertEquals(1, active.size());
        assertEquals("CS-A1", active.get(0).identity().toString());
    }
}
