package eapli.aisafe.fleet.domain;

import eapli.aisafe.company.domain.CompanyId;
import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Physical aircraft belonging to a company's operational fleet.
 */
@Entity
@Table(name = "aircraft")
public class Aircraft implements AggregateRoot<RegistrationNumber> {

    @EmbeddedId
    private RegistrationNumber registrationNumber;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "owner_company_id", nullable = false))
    })
    private CompanyId ownerCompanyId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "aircraft_model_id", nullable = false))
    })
    private AircraftModelId aircraftModelId;

    @Embedded
    private AircraftRegistrationCountry registrationCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 32)
    private AircraftOperationalStatus operationalStatus;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "first_registration_date")
    private LocalDate firstRegistrationDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aircraft_cabin_seats",
            joinColumns = @JoinColumn(name = "registration_number", referencedColumnName = "registration_number"))
    private List<CabinSeatBlock> cabinSeatBlocks = new ArrayList<>();

    protected Aircraft() {
        // for ORM
    }

    Aircraft(final RegistrationNumber registrationNumber,
             final AircraftRegistrationCountry registrationCountry,
             final AircraftModelId aircraftModelId,
             final CompanyId ownerCompanyId,
             final List<CabinSeatBlock> cabinSeatBlocks,
             final LocalDate manufacturingDate,
             final LocalDate firstRegistrationDate,
             final AircraftOperationalStatus operationalStatus) {
        this.registrationNumber = java.util.Objects.requireNonNull(registrationNumber, "Registration is required");
        this.registrationCountry = java.util.Objects.requireNonNull(registrationCountry, "Country is required");
        this.aircraftModelId = java.util.Objects.requireNonNull(aircraftModelId, "Model is required");
        this.ownerCompanyId = java.util.Objects.requireNonNull(ownerCompanyId, "Owner company is required");
        this.operationalStatus = java.util.Objects.requireNonNull(operationalStatus, "Status is required");
        this.cabinSeatBlocks = new ArrayList<>(cabinSeatBlocks);
        this.manufacturingDate = manufacturingDate;
        this.firstRegistrationDate = firstRegistrationDate;
    }

    @Override
    public RegistrationNumber identity() {
        return registrationNumber;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof Aircraft)) {
            return false;
        }
        final var that = (Aircraft) other;
        return identity().equals(that.identity());
    }

    public CompanyId ownerCompanyId() {
        return ownerCompanyId;
    }

    public AircraftModelId aircraftModelId() {
        return aircraftModelId;
    }

    public AircraftRegistrationCountry registrationCountry() {
        return registrationCountry;
    }

    public AircraftOperationalStatus operationalStatus() {
        return operationalStatus;
    }

    public List<CabinSeatBlock> cabinSeatBlocks() {
        return Collections.unmodifiableList(cabinSeatBlocks);
    }

    public int totalConfiguredSeats() {
        return cabinSeatBlocks.stream().mapToInt(CabinSeatBlock::seatCount).sum();
    }

    public Optional<LocalDate> manufacturingDate() {
        return Optional.ofNullable(manufacturingDate);
    }

    public Optional<LocalDate> firstRegistrationDate() {
        return Optional.ofNullable(firstRegistrationDate);
    }

    /**
     * Reference date for fleet age rules: manufacturing date when present, otherwise first registration.
     */
    public Optional<LocalDate> ageReferenceDate() {
        if (manufacturingDate != null) {
            return Optional.of(manufacturingDate);
        }
        if (firstRegistrationDate != null) {
            return Optional.of(firstRegistrationDate);
        }
        return Optional.empty();
    }

    /**
     * Complete calendar years between {@link #ageReferenceDate()} and {@code asOf} (exclusive of fractional years).
     *
     * @throws IllegalStateException when no age reference is stored
     */
    public int ageInCompleteYears(final LocalDate asOf) {
        final LocalDate ref = ageReferenceDate()
                .orElseThrow(() -> new IllegalStateException("Manufacturing date or first registration date is required for age"));
        return (int) ChronoUnit.YEARS.between(ref, asOf);
    }

    /**
     * Whether this aircraft is included in US072a “active fleet by model” for the given company and model
     * (not decommissioned, same owner, same model).
     */
    public boolean isListedForActiveFleetByModel(final CompanyId companyId, final AircraftModelId modelId) {
        return ownerCompanyId.equals(companyId)
                && aircraftModelId.equals(modelId)
                && operationalStatus != AircraftOperationalStatus.RETIRED;
    }

    /**
     * Whether this aircraft is included in an active-fleet listing filtered by total configured passenger seats
     * (not retired, same owner, exact seat count).
     */
    public boolean isListedForActiveFleetByPassengerCapacity(final CompanyId companyId, final int passengerCapacity) {
        return ownerCompanyId.equals(companyId)
                && operationalStatus != AircraftOperationalStatus.RETIRED
                && totalConfiguredSeats() == passengerCapacity;
    }

    /**
     * Active fleet listing by complete years of age (manufacturing preferred over first registration).
     */
    public boolean isListedForActiveFleetByAge(final CompanyId companyId, final int ageYears, final LocalDate asOf) {
        if (!ownerCompanyId.equals(companyId) || operationalStatus == AircraftOperationalStatus.RETIRED) {
            return false;
        }
        return ageReferenceDate()
                .map(ref -> (int) ChronoUnit.YEARS.between(ref, asOf) == ageYears)
                .orElse(false);
    }

    public boolean isRetired() {
        return this.operationalStatus == AircraftOperationalStatus.RETIRED;
    }

    public void markRetired() {
        if (this.operationalStatus == AircraftOperationalStatus.RETIRED) {
            throw new IllegalStateException("Aircraft is already retired");
        }
        this.operationalStatus = AircraftOperationalStatus.RETIRED;
    }
}
