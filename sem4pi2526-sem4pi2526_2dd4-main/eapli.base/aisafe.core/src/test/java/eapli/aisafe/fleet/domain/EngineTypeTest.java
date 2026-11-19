package eapli.aisafe.fleet.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link EngineType} (engine model aggregate). */
@DisplayName("EngineType")
class EngineTypeTest {

    @Test
    @DisplayName("values() exposes all motorization kinds")
    void valuesShouldExposeAllConstants() {
        // Arrange
        final EngineType[] expected = {
                EngineType.TURBOPROP,
                EngineType.TURBOFAN,
                EngineType.TURBOJET,
                EngineType.RAMJET,
                EngineType.ELECTRIC_PROPELLER,
                EngineType.OTHER
        };

        // Act
        final EngineType[] actual = EngineType.values();

        // Assert
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    @DisplayName("valueOf resolves each constant by name")
    void valueOfShouldResolveEachConstant(final EngineType type) {
        // Act
        final EngineType resolved = EngineType.valueOf(type.name());

        // Assert
        assertEquals(type, resolved);
    }

    @Test
    @DisplayName("valueOf rejects unknown name")
    void valueOfShouldRejectUnknown() {
        assertThrows(IllegalArgumentException.class, () -> EngineType.valueOf("HYBRID"));
    }

    @Test
    @DisplayName("compareTo orders by declaration")
    void compareToShouldFollowDeclarationOrder() {
        assertTrue(EngineType.TURBOPROP.compareTo(EngineType.TURBOFAN) < 0);
        assertTrue(EngineType.TURBOFAN.compareTo(EngineType.TURBOJET) < 0);
        assertTrue(EngineType.ELECTRIC_PROPELLER.compareTo(EngineType.OTHER) < 0);
        assertEquals(0, EngineType.OTHER.compareTo(EngineType.OTHER));
    }

    @Test
    @DisplayName("name() matches constant identifier")
    void nameShouldMatchConstant() {
        for (final EngineType type : EngineType.values()) {
            assertEquals(type.name(), type.toString());
        }
    }
}
