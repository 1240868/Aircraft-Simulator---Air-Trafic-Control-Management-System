package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.fleet.application.PendingFlightsChecker;
import eapli.aisafe.fleet.domain.RegistrationNumber;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placeholder until flight scheduling is implemented. Can be used in tests via {@link #markPending}.
 */
public final class InMemoryPendingFlightsChecker implements PendingFlightsChecker {

    private final Set<String> pendingRegistrations = ConcurrentHashMap.newKeySet();

    @Override
    public boolean hasPendingFlights(final RegistrationNumber registration) {
        return pendingRegistrations.contains(registration.toString());
    }

    /** For tests or future flight module integration. */
    public void markPending(final RegistrationNumber registration) {
        pendingRegistrations.add(registration.toString());
    }

    public void clearPending(final RegistrationNumber registration) {
        pendingRegistrations.remove(registration.toString());
    }
}
