package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Seat count allocated to one cabin class (e.g. ECONOMY, BUSINESS).
 */
@Embeddable
public class CabinSeatBlock implements ValueObject {

    @Column(name = "cabin_class", nullable = false, length = 40)
    private String cabinClass;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    protected CabinSeatBlock() {
        // for ORM
    }

    public CabinSeatBlock(final String cabinClass, final int seatCount) {
        Preconditions.nonEmpty(cabinClass, "Cabin class is required");
        Preconditions.ensure(seatCount >= 0, "Seat count cannot be negative");
        this.cabinClass = cabinClass.trim();
        this.seatCount = seatCount;
    }

    public String cabinClass() {
        return cabinClass;
    }

    public int seatCount() {
        return seatCount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (CabinSeatBlock) o;
        return seatCount == that.seatCount && cabinClass.equals(that.cabinClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cabinClass, seatCount);
    }
}
