package eapli.aisafe.fleet.application;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.domain.ManufacturerNormalizer;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Shared helpers for US072 fleet listing controllers. */
public final class FleetListing {

    private FleetListing() {
    }

    public static CompanyId requireCompanyOfUser(final AirTransportCompanyRepository companyRepo,
                                          final String username) {
        return companyRepo.findCompanyIdForCollaboratorUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Your user is not linked to any air transport company"));
    }

    static AircraftModel requireModel(final AircraftModelRepository modelRepo, final AircraftModelId id) {
        return modelRepo.ofIdentity(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown aircraft model: " + id));
    }

    static AircraftModel requireModelForAircraft(final AircraftModelRepository modelRepo, final Aircraft aircraft) {
        return modelRepo.ofIdentity(aircraft.aircraftModelId())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing aircraft model for aircraft " + aircraft.identity()));
    }

    static AircraftFleetListItemDTO toRow(final Aircraft aircraft, final AircraftModel model) {
        return new AircraftFleetListItemDTO(
                aircraft.identity().toString(),
                model.modelName(),
                model.manufacturer(),
                aircraft.operationalStatus().name(),
                aircraft.totalConfiguredSeats(),
                null);
    }

    static AircraftFleetListItemDTO toRowWithAge(final Aircraft aircraft,
                                                 final AircraftModel model,
                                                 final LocalDate asOf) {
        return new AircraftFleetListItemDTO(
                aircraft.identity().toString(),
                model.modelName(),
                model.manufacturer(),
                aircraft.operationalStatus().name(),
                aircraft.totalConfiguredSeats(),
                aircraft.ageInCompleteYears(asOf));
    }

    static boolean isKnownManufacturer(final AircraftModelRepository modelRepo, final String manufacturerNormalized) {
        for (final AircraftModel m : modelRepo.findAll()) {
            if (ManufacturerNormalizer.normalize(m.manufacturer()).equalsIgnoreCase(manufacturerNormalized)) {
                return true;
            }
        }
        return false;
    }

    static List<Aircraft> activeByManufacturer(final AircraftRepository aircraftRepo,
                                               final AircraftModelRepository modelRepo,
                                               final CompanyId companyId,
                                               final String manufacturerNormalized) {
        final List<Aircraft> out = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findByOwnerCompanyId(companyId)) {
            if (a.isRetired()) {
                continue;
            }
            final AircraftModel m = modelRepo.ofIdentity(a.aircraftModelId()).orElse(null);
            if (m != null && ManufacturerNormalizer.normalize(m.manufacturer())
                    .equalsIgnoreCase(manufacturerNormalized)) {
                out.add(a);
            }
        }
        return out;
    }
}
