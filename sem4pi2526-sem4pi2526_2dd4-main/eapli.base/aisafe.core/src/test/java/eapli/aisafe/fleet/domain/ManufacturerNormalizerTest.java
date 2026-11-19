package eapli.aisafe.fleet.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ManufacturerNormalizerTest {

    @Test
    public void normalizeShouldTrimAndCollapseSpaces() {
        assertEquals("Airbus", ManufacturerNormalizer.normalize("  Airbus  "));
        assertEquals("McDonnell Douglas", ManufacturerNormalizer.normalize("  McDonnell   Douglas  "));
    }

    @Test
    public void normalizeShouldCollapseTabsAndNewlines() {
        assertEquals("Bo ing", ManufacturerNormalizer.normalize("Bo\t\ning"));
        assertEquals("Emb raer", ManufacturerNormalizer.normalize("Emb\n  raer"));
    }

    @Test
    public void normalizeShouldTrimAroundLetters() {
        assertEquals("X", ManufacturerNormalizer.normalize("   X   "));
    }

    @Test
    public void normalizeShouldReturnEmptyWhenOnlyWhitespace() {
        assertEquals("", ManufacturerNormalizer.normalize("   "));
        assertEquals("", ManufacturerNormalizer.normalize("\t\n"));
    }

    @Test
    public void normalizeShouldRejectNull() {
        final NullPointerException ex = assertThrows(NullPointerException.class,
                () -> ManufacturerNormalizer.normalize(null));
        assertTrue(ex.getMessage().contains("Manufacturer"));
    }

    @Test
    public void normalizeShouldPreserveCasingAfterCollapse() {
        assertEquals("airbus", ManufacturerNormalizer.normalize("airbus"));
        assertEquals("BOEING", ManufacturerNormalizer.normalize("  BOEING  "));
    }

    @Test
    public void normalizeShouldTreatSingleCharacterAsValid() {
        assertEquals("A", ManufacturerNormalizer.normalize(" A "));
    }
}
