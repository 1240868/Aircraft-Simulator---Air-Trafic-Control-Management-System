package eapli.aisafe.fleet.application;

import eapli.aisafe.fleet.domain.RegistrationNumber;

/**
 * Abstraction for "pending flights" blocking fleet changes. Until the flight aggregate exists,
 * the default in-memory implementation reports none.
 */
public interface PendingFlightsChecker {

    boolean hasPendingFlights(RegistrationNumber registration);
}
