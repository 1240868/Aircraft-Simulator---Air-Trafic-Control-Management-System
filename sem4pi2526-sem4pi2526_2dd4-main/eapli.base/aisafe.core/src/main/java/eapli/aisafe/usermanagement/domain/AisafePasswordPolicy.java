package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;

public class AisafePasswordPolicy implements PasswordPolicy {
    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            return false;
        }
        return true;
    }
}
