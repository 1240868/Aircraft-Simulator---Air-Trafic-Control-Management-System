package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;

@Embeddable
public class JetEngineSpecs implements ValueObject {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "sea_level_static_thrust_value", nullable = true)),
            @AttributeOverride(name = "unit", column = @Column(name = "sea_level_static_thrust_unit", nullable = true))
    })
    private Thrust atSeaLevelStatic;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "cruise_thrust_value", nullable = true)),
            @AttributeOverride(name = "unit", column = @Column(name = "cruise_thrust_unit", nullable = true))
    })
    private Thrust atCruiseSpeed;

    @Column(name = "jet_tsfc", nullable = true)
    private double tsfc;

    protected JetEngineSpecs() {
        // for ORM
    }

    public JetEngineSpecs(final Thrust atSeaLevelStatic, final Thrust atCruiseSpeed, final double tsfc) {
        this.atSeaLevelStatic = java.util.Objects.requireNonNull(atSeaLevelStatic, "Sea level static thrust is required");
        this.atCruiseSpeed = java.util.Objects.requireNonNull(atCruiseSpeed, "Cruise speed thrust is required");
        Preconditions.ensure(tsfc > 0, "TSFC must be positive");
        Preconditions.ensure(atCruiseSpeed.value() <= atSeaLevelStatic.value(),
                "Cruise thrust cannot exceed sea level static thrust");
        this.tsfc = tsfc;
    }

    public Thrust atSeaLevelStatic() {
        return atSeaLevelStatic;
    }

    public Thrust atCruiseSpeed() {
        return atCruiseSpeed;
    }

    public double tsfc() {
        return tsfc;
    }
}

