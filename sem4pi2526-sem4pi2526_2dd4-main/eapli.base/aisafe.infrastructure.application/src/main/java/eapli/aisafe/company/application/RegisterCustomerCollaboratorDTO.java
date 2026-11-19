package eapli.aisafe.company.application;

/**
 * Input for registering a collaborator linked to a customer company.
 */
public record RegisterCustomerCollaboratorDTO(
        String companyId,
        String username,
        String password,
        String email,
        String firstName,
        String lastName,
        String phone) {
}
