package eapli.aisafe.aircontrol.application;

import eapli.aisafe.aircontrol.domain.AirControlArea;
import eapli.aisafe.aircontrol.domain.AirControlAreaCode;
import eapli.aisafe.aircontrol.repositories.AirControlAreaRepository;
import eapli.aisafe.application.security.AuthorizationPort;
import eapli.aisafe.application.security.FrameworkAuthorizationAdapter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;

@UseCaseController
public class RegisterAirControlAreaController {

    private final AuthorizationPort authz;
    private final AirControlAreaRepository repo;

    public RegisterAirControlAreaController() {
        this(FrameworkAuthorizationAdapter.usingRegistry(),
                PersistenceContext.repositories().airControlAreas());
    }

    public RegisterAirControlAreaController(final AuthorizationPort authz,
                                            final AirControlAreaRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    public AirControlArea registerAirControlArea(final String code, final String name,
                                                  final double latMin, final double latMax,
                                                  final double lonMin, final double lonMax) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN);

        if (repo.findByCode(AirControlAreaCode.valueOf(code)).isPresent()) {
            throw new IntegrityViolationException("Air control area code already exists: " + code);
        }

        final var area = new AirControlArea(code, name, latMin, latMax, lonMin, lonMax);
        return repo.save(area);
    }
}
