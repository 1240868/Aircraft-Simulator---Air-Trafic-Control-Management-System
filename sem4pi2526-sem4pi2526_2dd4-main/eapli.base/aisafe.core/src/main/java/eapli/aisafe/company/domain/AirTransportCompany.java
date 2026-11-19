package eapli.aisafe.company.domain;

import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "air_transport_companies")
public class AirTransportCompany implements AggregateRoot<CompanyId> {

    @EmbeddedId
    private CompanyId id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "company_name", nullable = false, length = 200))
    })
    private CompanyName name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "company_iata", nullable = false, length = 2))
    })
    private CompanyIataCode iataCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "company_icao", nullable = false, length = 3))
    })
    private CompanyIcaoCode icaoCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "number", column = @Column(name = "cert_number", nullable = false)),
            @AttributeOverride(name = "expiryDate", column = @Column(name = "cert_expiry", nullable = false))
    })
    private AirOperatorCertificate certificate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "air_transport_company_collaborators",
            joinColumns = @JoinColumn(name = "company_id", referencedColumnName = "company_id"))
    private Set<CustomerCollaboratorRef> collaborators = new HashSet<>();

    protected AirTransportCompany() {
        // for ORM
    }

    public AirTransportCompany(final CompanyName name,
                               final CompanyIataCode iataCode,
                               final CompanyIcaoCode icaoCode,
                               final AirOperatorCertificate certificate) {
        this.id = CompanyId.newId();
        this.name = java.util.Objects.requireNonNull(name, "Name is required");
        this.iataCode = java.util.Objects.requireNonNull(iataCode, "IATA code is required");
        this.icaoCode = java.util.Objects.requireNonNull(icaoCode, "ICAO code is required");
        this.certificate = Objects.requireNonNull(certificate, "Certificate is required");
        this.collaborators = new HashSet<>();
    }

    @Override
    public CompanyId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AirTransportCompany)) {
            return false;
        }
        final var that = (AirTransportCompany) other;
        return identity().equals(that.identity());
    }

    public CompanyName name() {
        return name;
    }

    public CompanyIataCode iataCode() {
        return iataCode;
    }

    public CompanyIcaoCode icaoCode() {
        return icaoCode;
    }

    public AirOperatorCertificate certificate() {
        return certificate;
    }

    /**
     * System usernames ({@code SystemUser}) authorised as collaborators for this company.
     */
    public Set<String> collaboratorUsernames() {
        if (collaborators == null) {
            return Set.of();
        }
        return collaborators.stream()
                .filter(Objects::nonNull)
                .map(CustomerCollaboratorRef::username)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public List<String> listActiveCollaborators() {
        return List.copyOf(collaboratorUsernames());
    }

    /**
     * Registers a collaborator username for tests and legacy callers without phone metadata.
     */
    public void addCollaborator(final String systemUsername) {
        addCollaborator(systemUsername, "900000000");
    }

    public void addCollaborator(final String systemUsername, final String phone) {
        if (collaborators == null) {
            collaborators = new HashSet<>();
        }
        if (!collaborators.add(new CustomerCollaboratorRef(systemUsername, phone))) {
            throw new IllegalStateException("Collaborator is already registered for this company");
        }
    }
}
