package eapli.aisafe.pilot.application;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.application.dto.PilotRosterListItemDTO;

import java.util.ArrayList;
import java.util.List;

/** Shared helpers for pilot roster listing. */
public final class PilotRosterListing {

    private PilotRosterListing() {
    }

    public static CompanyId requireCompanyOfUser(final AirTransportCompanyRepository companyRepo,
                                                 final String username) {
        return companyRepo.findCompanyIdForCollaboratorUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user is not linked to any air transport company"));
    }

    public static PilotRosterListItemDTO toRow(final Pilot pilot,
                                               final AircraftModelRepository modelRepo) {
        return new PilotRosterListItemDTO(
                pilot.identity().toString(),
                pilot.getName().firstName() + " " + pilot.getName().lastName(),
                pilot.getEmail().value(),
                pilot.getCertification().value(),
                pilot.getLicenseVoo().value(),
                pilot.getLicenseVoo().expirationDate().toString(),
                pilot.getStatus().name(),
                resolveCertifiedModelLabels(pilot, modelRepo));
    }

    static List<String> resolveCertifiedModelLabels(final Pilot pilot,
                                                    final AircraftModelRepository modelRepo) {
        final List<String> labels = new ArrayList<>();
        for (final AircraftModelId modelId : pilot.getCertifiedModels()) {
            labels.add(modelRepo.findById(modelId)
                    .map(model -> model.modelName() + " / " + model.manufacturer())
                    .orElse(modelId.toString()));
        }
        return List.copyOf(labels);
    }
}
