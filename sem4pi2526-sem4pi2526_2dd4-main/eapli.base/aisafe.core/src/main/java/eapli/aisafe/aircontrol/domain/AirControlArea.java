package eapli.aisafe.aircontrol.domain;

import eapli.aisafe.company.domain.CustomerCollaboratorRef;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "air_control_areas")
public class AirControlArea implements AggregateRoot<AirControlAreaCode> {

    @EmbeddedId
    private AirControlAreaCode code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latMin;

    @Column(nullable = false)
    private double latMax;

    @Column(nullable = false)
    private double lonMin;

    @Column(nullable = false)
    private double lonMax;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "air_control_area_collaborators",
            joinColumns = @JoinColumn(name = "aca_code", referencedColumnName = "air_control_area_code"))
    private Set<CustomerCollaboratorRef> collaborators = new HashSet<>();

    protected AirControlArea() {
    }

    public AirControlArea(final String code, final String name,
                          final double latMin, final double latMax,
                          final double lonMin, final double lonMax) {
        Preconditions.nonEmpty(name, "Name is required");
        Preconditions.ensure(latMin >= -90 && latMin <= 90, "Invalid latMin");
        Preconditions.ensure(latMax >= -90 && latMax <= 90, "Invalid latMax");
        Preconditions.ensure(latMin < latMax, "latMin must be less than latMax");
        Preconditions.ensure(lonMin >= -180 && lonMin <= 180, "Invalid lonMin");
        Preconditions.ensure(lonMax >= -180 && lonMax <= 180, "Invalid lonMax");
        Preconditions.ensure(lonMin < lonMax, "lonMin must be less than lonMax");

        this.code = AirControlAreaCode.valueOf(code);
        this.name = name;
        this.latMin = latMin;
        this.latMax = latMax;
        this.lonMin = lonMin;
        this.lonMax = lonMax;
        this.collaborators = new HashSet<>();
    }

    @Override
    public AirControlAreaCode identity() {
        return code;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AirControlArea)) {
            return false;
        }
        return identity().equals(((AirControlArea) other).identity());
    }

    public String name() {
        return name;
    }

    public AirControlAreaCode areaCode() {
        return code;
    }

    public double latMin() {
        return latMin;
    }

    public double latMax() {
        return latMax;
    }

    public double lonMin() {
        return lonMin;
    }

    public double lonMax() {
        return lonMax;
    }

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

    public void addCollaborator(final String systemUsername) {
        addCollaborator(systemUsername, "900000000");
    }

    public void addCollaborator(final String systemUsername, final String phone) {
        if (collaborators == null) {
            collaborators = new HashSet<>();
        }
        if (!collaborators.add(new CustomerCollaboratorRef(systemUsername, phone))) {
            throw new IllegalStateException("Collaborator is already registered for this air control area");
        }
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
