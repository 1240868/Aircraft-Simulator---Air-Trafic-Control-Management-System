package eapli.aisafe.company.application;

/**
 * Data for presenting a customer's collaborator to the backoffice UI.
 *
 * Note: phone may be null if not stored by the underlying user model.
 */
public record CollaboratorDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        String phone) {
}

