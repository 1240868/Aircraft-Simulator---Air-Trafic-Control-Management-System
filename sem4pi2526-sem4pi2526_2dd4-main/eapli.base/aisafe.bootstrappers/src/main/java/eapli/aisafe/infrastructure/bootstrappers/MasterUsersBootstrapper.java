package eapli.aisafe.infrastructure.bootstrappers;

import eapli.aisafe.roles.Roles;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.util.HashSet;
import java.util.Set;

public class MasterUsersBootstrapper extends AbstractUserBootstrapper implements Action {

    @Override
    public boolean execute() {
        registerAdmin("admin", "Admin123!", "Admin", "System", "admin@aisafe.com");
        registerWeatherPerson("weather", "Weather123!", "Weather", "Person", "weather@aisafe.com");
        registerPilot("pilot1", "Pilot123!", "John", "Doe", "pilot@aisafe.com");
        registerFlightControlOperator("fco1", "Fco12345!", "Flight", "Operator", "fco@aisafe.com");
        return true;
    }

    private void registerAdmin(final String username, final String password,
            final String firstName, final String lastName, final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(Roles.ADMIN);
        registerUser(username, password, firstName, lastName, email, roles);
    }

    private void registerWeatherPerson(final String username, final String password,
                                       final String firstName, final String lastName, final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(Roles.WEATHER_PERSON);
        registerUser(username, password, firstName, lastName, email, roles);
    }

    private void registerPilot(final String username, final String password,
                               final String firstName, final String lastName, final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(Roles.PILOT);
        registerUser(username, password, firstName, lastName, email, roles);
    }

    private void registerFlightControlOperator(final String username, final String password,
                                               final String firstName, final String lastName, final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(Roles.FLIGHT_CONTROL_OPERATOR);
        registerUser(username, password, firstName, lastName, email, roles);
    }
}
