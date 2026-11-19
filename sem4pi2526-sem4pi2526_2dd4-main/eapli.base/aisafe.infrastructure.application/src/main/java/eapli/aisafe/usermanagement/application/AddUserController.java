package eapli.aisafe.usermanagement.application;

import eapli.aisafe.roles.Roles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;
import java.util.Set;

@UseCaseController
public class AddUserController {
    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();

    public Role[] getRoleTypes() {
        return new Role[]{
            Roles.ADMIN,
            Roles.BACKOFFICE_OPERATOR,
            Roles.PILOT,
            Roles.FLIGHT_CONTROL_OPERATOR,
            Roles.WEATHER_PERSON,
            Roles.AIR_TRANSPORT_COMPANY_COLLABORATOR,
            Roles.AIR_CONTROL_AREA_COLLABORATOR
        };
    }

    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles) {
        authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN);
        return userSvc.registerNewUser(username, password, firstName, lastName, email, roles,
                CurrentTimeCalendars.now());
    }
}
