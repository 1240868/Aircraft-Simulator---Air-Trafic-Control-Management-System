package eapli.aisafe.fleet.domain;

import java.util.Objects;

/**
 * Normalizes manufacturer names for fleet listing comparisons (US072b).
 */
public final class ManufacturerNormalizer {

    private ManufacturerNormalizer() {
    }

    public static String normalize(final String raw) {
        return Objects.requireNonNull(raw, "Manufacturer is required").strip().replaceAll("\\s+", " ");
    }
}
