package eapli.aisafe.flightPlan.repositories;

import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.pilot.PilotId;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Collection;
import java.util.Optional;

public interface FlightPlanRepository extends DomainRepository<FlightPlanId, FlightPlan> {
    Optional<FlightPlan>findById(FlightPlanId flightPlanId);
    /**
     * Find all flight plans by departure year and month.
     *
     * @param year the departure year
     * @param month the departure month (1-12)
     * @return an Iterable of FlightPlan objects matching the given year/month
     */
    Iterable<FlightPlan> findByDepartureYearMonth(int year, int month);

    boolean hasAssignedFlightPlansFor(PilotId pilotId);
}
