package eapli.aisafe.pilot;

import eapli.aisafe.company.domain.CompanyId;
import eapli.aisafe.fleet.domain.AircraftModelId;
import eapli.aisafe.sharedkernel.valueobjects.Email;
import eapli.aisafe.sharedkernel.valueobjects.FullName;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pilots")
public class Pilot implements AggregateRoot<PilotId> {
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PilotStatus status;
    @EmbeddedId
    private PilotId id;
    @Embedded
    private FullName name;
    @Embedded
    private Email email;
    @Embedded
    private CompanyId company;

    @Embedded
    private PilotCertification certification;
    @Embedded
    private License licenseVoo;

    @ElementCollection
    @CollectionTable(name = "pilot_certified_models", joinColumns = @JoinColumn(name = "pilot_id"))
    @AttributeOverride(name = "value", column = @Column(name = "model_id"))
    private List<AircraftModelId> certifiedModels = new ArrayList<>();

    public Pilot(PilotId id, FullName name, Email email, CompanyId company,
                 PilotCertification certification, License licenseVoo,
                 List<AircraftModelId> certifiedModels) {
        Preconditions.nonNull(certifiedModels, "Certified models list is required");
        Preconditions.ensure(!certifiedModels.isEmpty(), "A pilot must be certified for at least one aircraft model");
        this.id = id;
        this.name = name;
        this.email = email;
        this.company = company;
        this.certification = certification;
        this.licenseVoo = licenseVoo;
        this.certifiedModels = new ArrayList<>(certifiedModels);
        this.status = PilotStatus.ACTIVE;
    }

    protected Pilot() {
    }

    @Override
    public String toString() {
        return "Pilot{" +
                "status=" + status +
                ", id=" + id +
                ", name=" + name +
                ", email=" + email +
                ", certification=" + certification +
                ", licenseVoo=" + licenseVoo +
                ", company=" + company +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Pilot pilot = (Pilot) o;
        return Objects.equals(id, pilot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean sameAs(Object other) {
        if (!(other instanceof Pilot)) {
            return false;
        }
        final Pilot that = (Pilot) other;
        return this.email.value().equals(that.email.value());
    }

    @Override
    public int compareTo(PilotId other) {
        return AggregateRoot.super.compareTo(other);
    }

    public PilotStatus getStatus() {
        return status;
    }

    public void setStatus(PilotStatus status) {
        this.status = status;
    }

    public PilotId getId() {
        return id;
    }

    public void setId(PilotId id) {
        this.id = id;
    }

    public PilotCertification getCertification() {
        return certification;
    }

    public void setCertification(PilotCertification certification) {
        this.certification = certification;
    }

    public License getLicenseVoo() {
        return licenseVoo;
    }

    public void setLicenseVoo(License licenseVoo) {
        this.licenseVoo = licenseVoo;
    }

    public FullName getName() { return name; }
    public void setName(final FullName name) { this.name = name; }
    public Email getEmail() { return email; }
    public void setEmail(final Email email) { this.email = email; }
    public CompanyId getCompany() { return company; }
    public void setCompany(final CompanyId company) { this.company = company; }
    public List<AircraftModelId> getCertifiedModels() { return Collections.unmodifiableList(certifiedModels); }

    /** Only active pilots appear on the company roster. */
    public boolean isEligibleForRosterListing() {
        return status == PilotStatus.ACTIVE;
    }

    /**
     * Deactivates the pilot (status INACTIVE) when no flight plans are assigned.
     *
     * @param hasAssignedFlightPlans whether the pilot has flight plans assigned
     */
    public void deactivate(final boolean hasAssignedFlightPlans) {
        if (status != PilotStatus.ACTIVE) {
            throw new IllegalStateException("Only active pilots can be deactivated");
        }
        if (hasAssignedFlightPlans) {
            throw new IllegalStateException("Pilot has assigned flight plans and cannot be deactivated");
        }
        this.status = PilotStatus.INACTIVE;
    }

    @Override
    public PilotId identity() {
        return id;
    }

    @Override
    public boolean hasIdentity(PilotId id) {
        return AggregateRoot.super.hasIdentity(id);
    }
}
