package eapli.aisafe.fleet.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "aircraft_models")
public class AircraftModel implements AggregateRoot<AircraftModelId> {

    @EmbeddedId
    private AircraftModelId id;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngineType motorization;

    @Column(nullable = false)
    private int numberOfEngines;

    /**
     * Maximum passenger seating capacity certified for this model (upper bound for cabin configuration).
     */
    @Column(nullable = false)
    private int passengerCapacity;

    @Column(nullable = false)
    private double maximumRangeKm;

    @Column(nullable = false)
    private double emptyWeightKg;

    @Column(nullable = false)
    private double mtowKg;

    @Column(nullable = false)
    private double mzfwKg;

    @Column(nullable = false)
    private double maxFuelCapacityLiters;

    @Column(nullable = false)
    private double serviceCeilingM;

    @Column(nullable = false)
    private double cruiseSpeedMs;

    @Column(nullable = false)
    private double wingAreaM2;

    @Column(nullable = false)
    private double cd0;

    @Column(nullable = false)
    private double cl;

    @ElementCollection
    @CollectionTable(
            name = "aircraft_model_certified_engines",
            joinColumns = @JoinColumn(name = "aircraft_model_id")
    )
    @Column(name = "engine_model_id", nullable = false)
    private List<String> certifiedEngineModelIds = new ArrayList<>();

    protected AircraftModel() {
    }

    /**
     * Convenience constructor for tests and fleet/weather use cases that do not model mass/aero coefficients.
     * Delegates to the full constructor with neutral placeholder values.
     */
    public AircraftModel(final String modelName,
                         final String manufacturer,
                         final AircraftType type,
                         final EngineType motorization,
                         final int numberOfEngines,
                         final int passengerCapacity,
                         final double maximumRangeKm,
                         final List<EngineModelId> certifiedEngines) {
        this(modelName, manufacturer, type, motorization, numberOfEngines, passengerCapacity, maximumRangeKm,
                42_000.0, 78_000.0, 62_000.0, 19_000.0, 12_500.0, 230.0, 122.6, 0.02, 1.5,
                certifiedEngines);
    }

    public AircraftModel(final String modelName,
                         final String manufacturer,
                         final AircraftType type,
                         final EngineType motorization,
                         final int numberOfEngines,
                         final int passengerCapacity,
                         final double maximumRangeKm,
                         final double emptyWeightKg,
                         final double mtowKg,
                         final double mzfwKg,
                         final double maxFuelCapacityLiters,
                         final double serviceCeilingM,
                         final double cruiseSpeedMs,
                         final double wingAreaM2,
                         final double cd0,
                         final double cl,
                         final List<EngineModelId> certifiedEngines) {
        this.id = AircraftModelId.newId();
        Preconditions.nonEmpty(modelName, "Model name is required");
        this.modelName = modelName;
        Preconditions.nonEmpty(manufacturer, "Manufacturer is required");
        this.manufacturer = manufacturer;
        this.type = java.util.Objects.requireNonNull(type, "Aircraft type is required");
        this.motorization = java.util.Objects.requireNonNull(motorization, "Motorization is required");
        Preconditions.ensure(numberOfEngines > 0, "Number of engines must be positive");
        this.numberOfEngines = numberOfEngines;
        Preconditions.ensure(passengerCapacity > 0, "Passenger capacity must be positive");
        this.passengerCapacity = passengerCapacity;
        Preconditions.ensure(maximumRangeKm > 0, "Maximum range must be positive");
        this.maximumRangeKm = maximumRangeKm;
        Preconditions.ensure(emptyWeightKg > 0, "Empty weight must be positive");
        this.emptyWeightKg = emptyWeightKg;
        Preconditions.ensure(mtowKg > 0, "MTOW must be positive");
        this.mtowKg = mtowKg;
        Preconditions.ensure(mzfwKg > 0, "MZFW must be positive");
        this.mzfwKg = mzfwKg;
        Preconditions.ensure(maxFuelCapacityLiters > 0, "Max fuel capacity must be positive");
        this.maxFuelCapacityLiters = maxFuelCapacityLiters;
        Preconditions.ensure(serviceCeilingM > 0, "Service ceiling must be positive");
        this.serviceCeilingM = serviceCeilingM;
        Preconditions.ensure(cruiseSpeedMs > 0, "Cruise speed must be positive");
        this.cruiseSpeedMs = cruiseSpeedMs;
        Preconditions.ensure(wingAreaM2 > 0, "Wing area must be positive");
        this.wingAreaM2 = wingAreaM2;
        Preconditions.ensure(cd0 > 0, "Drag coefficient (Cd0) must be positive");
        this.cd0 = cd0;
        Preconditions.ensure(cl > 0, "Lift coefficient (Cl) must be positive");
        this.cl = cl;
        Preconditions.ensure(certifiedEngines != null && !certifiedEngines.isEmpty(),
                "At least one certified engine model is required");
        this.certifiedEngineModelIds = new ArrayList<>();
        for (final var engineId : certifiedEngines) {
            this.certifiedEngineModelIds.add(engineId.toString());
        }
    }

    @Override
    public AircraftModelId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AircraftModel)) return false;
        final var that = (AircraftModel) other;
        return identity().equals(that.identity());
    }

    public String modelName() { return modelName; }
    public String manufacturer() { return manufacturer; }
    public AircraftType type() { return type; }
    public double maximumRangeKm() { return maximumRangeKm; }
    public EngineType motorization() { return motorization; }
    public int numberOfEngines() { return numberOfEngines; }
    public int passengerCapacity() { return passengerCapacity; }
    public double emptyWeightKg() { return emptyWeightKg; }
    public double mtowKg() { return mtowKg; }
    public double mzfwKg() { return mzfwKg; }
    public double maxFuelCapacityLiters() { return maxFuelCapacityLiters; }
    public double serviceCeilingM() { return serviceCeilingM; }
    public double cruiseSpeedMs() { return cruiseSpeedMs; }
    public double wingAreaM2() { return wingAreaM2; }
    public double cd0() { return cd0; }
    public double cl() { return cl; }

    public List<EngineModelId> certifiedEngineModelIds() {
        final List<EngineModelId> ids = new ArrayList<>();
        for (final var s : certifiedEngineModelIds) {
            ids.add(new EngineModelId(java.util.UUID.fromString(s)));
        }
        return Collections.unmodifiableList(ids);
    }
}
