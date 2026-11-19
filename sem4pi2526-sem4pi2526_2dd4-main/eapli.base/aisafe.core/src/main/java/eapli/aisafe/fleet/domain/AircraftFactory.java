package eapli.aisafe.fleet.domain;

import eapli.aisafe.company.domain.CompanyId;
import eapli.framework.validations.Preconditions;
import java.time.LocalDate;
import java.util.List;

/**
 * Creates {@link Aircraft} instances enforcing cabin capacity against the model limit.
 */
public final class AircraftFactory {

    private AircraftFactory() {
    }

    public static Aircraft registerNew(final String registrationNumberRaw,
                                         final String countryIsoAlpha2,
                                         final AircraftModelId modelId,
                                         final CompanyId ownerCompanyId,
                                         final List<CabinSeatBlock> cabinBreakdown,
                                         final int modelPassengerCapacity,
                                         final LocalDate manufacturingDate,
                                         final LocalDate firstRegistrationDate) {
        return registerWithOperationalStatus(
                registrationNumberRaw, countryIsoAlpha2, modelId, ownerCompanyId,
                cabinBreakdown, modelPassengerCapacity, manufacturingDate, firstRegistrationDate,
                AircraftOperationalStatus.ACTIVE);
    }

    /**
     * Same cabin validation as {@link #registerNew}, but allows choosing the initial operational status
     * (e.g. {@link AircraftOperationalStatus#MAINTENANCE} for listings / tests).
     */
    public static Aircraft registerWithOperationalStatus(final String registrationNumberRaw,
                                                         final String countryIsoAlpha2,
                                                         final AircraftModelId modelId,
                                                         final CompanyId ownerCompanyId,
                                                         final List<CabinSeatBlock> cabinBreakdown,
                                                         final int modelPassengerCapacity,
                                                         final LocalDate manufacturingDate,
                                                         final LocalDate firstRegistrationDate,
                                                         final AircraftOperationalStatus initialStatus) {
        Preconditions.ensure(modelPassengerCapacity > 0, "Model passenger capacity must be positive");
        Preconditions.ensure(cabinBreakdown != null && !cabinBreakdown.isEmpty(),
                "At least one cabin class seat allocation is required");
        final int total = cabinBreakdown.stream().mapToInt(CabinSeatBlock::seatCount).sum();
        Preconditions.ensure(total > 0, "At least one seat must be configured");
        Preconditions.ensure(total <= modelPassengerCapacity,
                "Total cabin seats (" + total + ") cannot exceed model capacity (" + modelPassengerCapacity + ")");
        Preconditions.ensure(manufacturingDate != null || firstRegistrationDate != null,
                "Manufacturing date or first registration date is required");

        final LocalDate today = LocalDate.now();
        if (manufacturingDate != null && manufacturingDate.isAfter(today)) {
            throw new IllegalArgumentException("Manufacturing date cannot be in the future");
        }
        if (firstRegistrationDate != null && firstRegistrationDate.isAfter(today)) {
            throw new IllegalArgumentException("First registration date cannot be in the future");
        }

        final RegistrationNumber reg = RegistrationNumber.valueOf(registrationNumberRaw);
        final AircraftRegistrationCountry country = AircraftRegistrationCountry.valueOf(countryIsoAlpha2);
        return new Aircraft(reg, country, modelId, ownerCompanyId,
                List.copyOf(cabinBreakdown), manufacturingDate, firstRegistrationDate,
                java.util.Objects.requireNonNull(initialStatus, "Status is required"));
    }
}
