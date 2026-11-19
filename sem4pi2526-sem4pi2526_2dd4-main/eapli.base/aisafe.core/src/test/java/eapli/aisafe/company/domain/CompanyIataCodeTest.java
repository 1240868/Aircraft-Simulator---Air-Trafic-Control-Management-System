package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CompanyIataCodeTest {

    @Test
    public void shouldCreateValidCode() {
        final var code = new CompanyIataCode("TP");
        assertNotNull(code);
    }

    @Test
    public void shouldConvertToUpperCase() {
        final var code = new CompanyIataCode("tp");
        assertEquals("TP", code.value());
    }

    @Test
    public void shouldTrimWhitespace() {
        final var code = new CompanyIataCode("  TP  ");
        assertEquals("TP", code.value());
    }

    @Test
    public void valueShouldReturnCode() {
        final var code = new CompanyIataCode("TP");
        assertEquals("TP", code.value());
    }

    @Test
    public void shouldRejectNull() {
        assertThrows(Exception.class, () -> new CompanyIataCode(null));
    }

    @Test
    public void shouldRejectEmpty() {
        assertThrows(Exception.class, () -> new CompanyIataCode(""));
    }

    @Test
    public void shouldRejectBlank() {
        assertThrows(Exception.class, () -> new CompanyIataCode("   "));
    }

    @Test
    public void shouldRejectSingleLetter() {
        assertThrows(Exception.class, () -> new CompanyIataCode("T"));
    }

    @Test
    public void shouldRejectThreeLetters() {
        assertThrows(Exception.class, () -> new CompanyIataCode("TAP"));
    }

    @Test
    public void shouldRejectDigits() {
        assertThrows(Exception.class, () -> new CompanyIataCode("12"));
    }

    @Test
    public void shouldRejectMixedAlphaDigit() {
        assertThrows(Exception.class, () -> new CompanyIataCode("T1"));
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final var ctor = CompanyIataCode.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var code = ctor.newInstance();
        assertNotNull(code);
    }
}
