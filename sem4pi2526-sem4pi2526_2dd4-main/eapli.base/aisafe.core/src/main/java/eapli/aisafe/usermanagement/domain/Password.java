package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;

public class Password implements PasswordPolicy {
    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            return false;
        }
        return true;
    }
}