package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "engine_models")
public class EngineModel implements AggregateRoot<EngineModelId> {

    @EmbeddedId
    private EngineModelId id;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngineType type;

    @Embedded
    private FuelSpecs fuelSpecs;

    @Embedded
    private JetEngineSpecs jetSpecs;

    @Embedded
    private ElectricEngineSpecs electricSpecs;

    protected EngineModel() {
    }

    private EngineModel(final String modelName, final String manufacturer,
                        final EngineType type, final FuelSpecs fuelSpecs,
                        final JetEngineSpecs jetSpecs, final ElectricEngineSpecs electricSpecs) {
        this.id = EngineModelId.newId();
        Preconditions.nonEmpty(modelName, "Model name is required");
        this.modelName = modelName;
        Preconditions.nonEmpty(manufacturer, "Manufacturer is required");
        this.manufacturer = manufacturer;
        this.type = java.util.Objects.requireNonNull(type, "Engine type is required");
        this.fuelSpecs = java.util.Objects.requireNonNull(fuelSpecs, "Fuel specs are required");
        this.jetSpecs = jetSpecs;
        this.electricSpecs = electricSpecs;
        ensureSpecsMatchType();
    }

    public static EngineModel createJet(final String modelName, final String manufacturer,
                                        final EngineType type, final FuelSpecs fuelSpecs,
                                        final JetEngineSpecs specs) {
        Preconditions.ensure(type != EngineType.ELECTRIC_PROPELLER,
                "Jet engine specs are not compatible with electric engines");
        return new EngineModel(modelName, manufacturer, type, fuelSpecs,
                java.util.Objects.requireNonNull(specs, "Jet specs are required"), null);
    }

    public static EngineModel createElectric(final String modelName, final String manufacturer,
                                             final FuelSpecs fuelSpecs, final ElectricEngineSpecs specs) {
        return new EngineModel(modelName, manufacturer, EngineType.ELECTRIC_PROPELLER, fuelSpecs,
                null, java.util.Objects.requireNonNull(specs, "Electric specs are required"));
    }

    private void ensureSpecsMatchType() {
        if (type == EngineType.ELECTRIC_PROPELLER) {
            Preconditions.ensure(electricSpecs != null, "Electric engines must have ElectricEngineSpecs");
            Preconditions.ensure(jetSpecs == null, "Electric engines cannot have JetEngineSpecs");
            Preconditions.ensure(fuelSpecs.type() == FuelType.ELECTRICITY, "Electric engines must use ELECTRICITY");
        } else {
            Preconditions.ensure(jetSpecs != null, "Jet engines must have JetEngineSpecs");
            Preconditions.ensure(electricSpecs == null, "Jet engines cannot have ElectricEngineSpecs");
            Preconditions.ensure(fuelSpecs.type() == FuelType.JET_A1, "Jet engines must use JET_A1");
        }
    }

    @Override
    public EngineModelId identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof EngineModel)) return false;
        return identity().equals(((EngineModel) other).identity());
    }

    public String modelName() { return modelName; }
    public String manufacturer() { return manufacturer; }
    public EngineType type() { return type; }
    public FuelSpecs fuelSpecs() { return fuelSpecs; }
    public boolean isCompatibleWith(final EngineType type) { return this.type == type; }
    public JetEngineSpecs jetSpecs() { return jetSpecs; }
    public ElectricEngineSpecs electricSpecs() { return electricSpecs; }
}
