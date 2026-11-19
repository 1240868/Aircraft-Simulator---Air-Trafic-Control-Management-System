package eapli.aisafe.fleet.application;

import java.util.List;

/**
 * Result of a fleet listing use case (US072a–d).
 */
public record FleetListResult(List<AircraftFleetListItemDTO> aircraft, String messageWhenEmpty) {

    public static final String NO_RESULTS_BY_MODEL =
            "There are no aircraft of the selected model in your company's active fleet.";
    public static final String NO_RESULTS_BY_MAKER =
            "There are no aircraft of the selected manufacturer in your company's active fleet.";
    public static final String NO_RESULTS_BY_CAPACITY =
            "There are no aircraft with the requested passenger capacity in your company's active fleet.";
    public static final String NO_RESULTS_BY_AGE =
            "There are no aircraft with the requested age in your company's active fleet.";

    public FleetListResult {
        aircraft = List.copyOf(aircraft);
    }

    public static FleetListResult of(final List<AircraftFleetListItemDTO> rows, final String emptyMessage) {
        if (rows.isEmpty()) {
            return new FleetListResult(rows, emptyMessage);
        }
        return new FleetListResult(rows, null);
    }
}
