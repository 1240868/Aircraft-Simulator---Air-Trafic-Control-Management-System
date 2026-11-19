package eapli.aisafe.app.user.console.presentation;

import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class LoginUI extends AbstractUI {
    private Role onlyWithThis;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private final int maxAttempts;
    private final CredentialHandler credentialHandler;

    public LoginUI(CredentialHandler credentialHandler) {
        maxAttempts = DEFAULT_MAX_ATTEMPTS;
        this.credentialHandler = credentialHandler;
    }

    public LoginUI(CredentialHandler credentialHandler, final Role onlyWithThis) {
        this.onlyWithThis = onlyWithThis;
        maxAttempts = DEFAULT_MAX_ATTEMPTS;
        this.credentialHandler = credentialHandler;
    }

    @Override
    protected boolean doShow() {
        var attempt = 1;
        while (attempt <= maxAttempts) {
            final String userName = Console.readNonEmptyLine("Username:", "Please provide a username");
            final String password = Console.readLine("Password:");
            if (credentialHandler.authenticated(userName, password, onlyWithThis)) {
                return true;
            }
            System.out.printf("Wrong username or password. You have %d attempts left.%n%n",
                    maxAttempts - attempt);
            attempt++;
        }
        System.out.println("Sorry, unable to authenticate. Please contact your administrator.");
        return false;
    }

    @Override
    public String headline() {
        return "Login";
    }
}
