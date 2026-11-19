package eapli.aisafe.aircontrol.application;

/**
 * Input for registering a collaborator linked to an air control area (customer).
 */
public record RegisterAreaCollaboratorDTO(
        String areaId,
        String username,
        String password,
        String email,
        String firstName,
        String lastName,
        String phone) {
}
