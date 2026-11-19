package eapli.aisafe.flightroute;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.sharedkernel.valueobjects.MeasurementUnit;

/** Computes great-circle distance and cruise-time estimates between airports (US073). */
public final class HaversineRouteDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CRUISE_SPEED_KMH = 850.0;

    private HaversineRouteDistanceCalculator() {
    }

    public static RouteDistance calculateDistance(final Airport origin, final Airport destination) {
        final double km = haversineKm(
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude());
        return new RouteDistance(km * 1000, MeasurementUnit.METER);
    }

    public static RouteDuration estimateDuration(final RouteDistance distance) {
        final double km = distance.value() / 1000.0;
        final double hours = km / CRUISE_SPEED_KMH;
        final long minutes = Math.max(1L, Math.round(hours * 60.0));
        return new RouteDuration(minutes);
    }

    private static double haversineKm(final double lat1, final double lon1,
                                      final double lat2, final double lon2) {
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
