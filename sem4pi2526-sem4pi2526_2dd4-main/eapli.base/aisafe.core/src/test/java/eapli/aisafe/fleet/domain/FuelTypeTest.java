package eapli.aisafe.fleet.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link FuelType} (engine model aggregate). */
@DisplayName("FuelType")
class FuelTypeTest {

    @Test
    @DisplayName("values() exposes jet and electric fuels")
    void valuesShouldExposeBothConstants() {
        // Arrange
        final FuelType[] expected = {FuelType.JET_A1, FuelType.ELECTRICITY};

        // Act
        final FuelType[] actual = FuelType.values();

        // Assert
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @EnumSource(FuelType.class)
    @DisplayName("valueOf resolves each constant by name")
    void valueOfShouldResolveEachConstant(final FuelType type) {
        // Act
        final FuelType resolved = FuelType.valueOf(type.name());

        // Assert
        assertEquals(type, resolved);
    }

    @Test
    @DisplayName("valueOf rejects unknown name")
    void valueOfShouldRejectUnknown() {
        assertThrows(IllegalArgumentException.class, () -> FuelType.valueOf("DIESEL"));
    }

    @Test
    @DisplayName("compareTo orders JET_A1 before ELECTRICITY")
    void compareToShouldOrderByDeclaration() {
        assertTrue(FuelType.JET_A1.compareTo(FuelType.ELECTRICITY) < 0);
        assertTrue(FuelType.ELECTRICITY.compareTo(FuelType.JET_A1) > 0);
        assertEquals(0, FuelType.JET_A1.compareTo(FuelType.JET_A1));
    }
}
