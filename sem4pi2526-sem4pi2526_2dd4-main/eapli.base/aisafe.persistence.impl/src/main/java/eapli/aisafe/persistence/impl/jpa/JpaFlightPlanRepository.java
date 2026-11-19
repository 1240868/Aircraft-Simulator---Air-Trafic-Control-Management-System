package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.flightPlan.domain.FlightLeg;
import eapli.aisafe.flightPlan.domain.FlightPlan;
import eapli.aisafe.flightPlan.domain.FlightPlanId;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.pilot.PilotId;

class JpaFlightPlanRepository
        extends BaseJpaRepositoryBase<FlightPlan, FlightPlanId, FlightPlanId>
        implements FlightPlanRepository {

    JpaFlightPlanRepository() {
        super("id");
    }

    @Override
    public FlightPlan save(final FlightPlan entity) {
        if (ofIdentity(entity.identity()).isEmpty()) {
            context().beginTransaction();
            try {
                entityManager().persist(entity);
                entityManager().flush();
                for (final FlightLeg leg : entity.getLegs()) {
                    entityManager().persist(leg);
                }
                context().commit();
            } catch (final RuntimeException e) {
                context().close();
                throw e;
            }
            context().close();
            return entity;
        }
        return super.save(entity);
    }

    @Override
    public java.util.Optional<FlightPlan> findById(final FlightPlanId flightPlanId) {
        return ofIdentity(flightPlanId);
    }

    @Override
    public boolean hasAssignedFlightPlansFor(final PilotId pilotId) {
        return matchOne("e.pilot.value = :pilotId", "pilotId", pilotId.toString()).isPresent();
    }

    @Override
    public Iterable<FlightPlan> findByDepartureYearMonth(final int year, final int month) {
        return match(
                "year(e.departureDateTime.value) = :y"
                + " AND month(e.departureDateTime.value) = :m",
                "y", year, "m", month);
    }
}
