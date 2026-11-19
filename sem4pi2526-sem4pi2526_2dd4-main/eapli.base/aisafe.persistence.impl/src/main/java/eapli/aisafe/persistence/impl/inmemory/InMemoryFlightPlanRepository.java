package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.pilot.PilotId;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryFlightPlanRepository
        extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
        implements FlightPlanRepository {

    @Override
    public java.util.Optional<FlightPlan> findById(final FlightPlanId flightPlanId) {
        return ofIdentity(flightPlanId);
    }

    @Override
    public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
        return matchOne(fp -> fp.getPilot() != null && fp.getPilot().equals(pilotId)).isPresent();
    }

    @Override
    public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
        return match(fp -> {
            final var dt = fp.getDepartureDateTime();
            if (dt == null) return false;
            final var ldt = dt.value();
            return ldt.getYear() == year && ldt.getMonthValue() == month;
        });
    }
}
