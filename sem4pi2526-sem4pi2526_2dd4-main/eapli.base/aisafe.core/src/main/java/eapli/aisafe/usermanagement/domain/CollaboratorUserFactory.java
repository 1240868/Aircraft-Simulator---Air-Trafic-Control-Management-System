package eapli.aisafe.usermanagement.domain;

import eapli.framework.util.Utility;

/**
 * Builds validated {@link CollaboratorUserData} from raw input.
 */
@Utility
public final class CollaboratorUserFactory {
    private CollaboratorUserFactory() {
    }

    public static CollaboratorUserData newCollaboratorUser(final String email,
                                                           final String firstName,
                                                           final String lastName,
                                                           final String phone) {
        return new CollaboratorUserData(
                new CollaboratorEmail(email),
                new CollaboratorFullName(firstName, lastName),
                new CollaboratorPhoneNumber(phone));
    }
}
