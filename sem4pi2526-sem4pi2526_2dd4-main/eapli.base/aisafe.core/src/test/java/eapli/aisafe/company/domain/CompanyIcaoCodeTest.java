package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CompanyIcaoCodeTest {

    @Test
    public void shouldCreateValidTwoLetterCode() {
        final var code = new CompanyIcaoCode("TP");
        assertEquals("TP", code.value());
    }

    @Test
    public void shouldCreateValidThreeLetterCode() {
        final var code = new CompanyIcaoCode("TAP");
        assertEquals("TAP", code.value());
    }

    @Test
    public void shouldConvertToUpperCase() {
        final var code = new CompanyIcaoCode("tap");
        assertEquals("TAP", code.value());
    }

    @Test
    public void shouldTrimWhitespace() {
        final var code = new CompanyIcaoCode("  TAP  ");
        assertEquals("TAP", code.value());
    }

    @Test
    public void valueShouldReturnCode() {
        final var code = new CompanyIcaoCode("AB");
        assertEquals("AB", code.value());
    }

    @Test
    public void shouldRejectNull() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode(null));
    }

    @Test
    public void shouldRejectEmpty() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode(""));
    }

    @Test
    public void shouldRejectBlank() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode("   "));
    }

    @Test
    public void shouldRejectSingleLetter() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode("T"));
    }

    @Test
    public void shouldRejectFourLetters() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode("TAPA"));
    }

    @Test
    public void shouldRejectDigits() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode("123"));
    }

    @Test
    public void shouldRejectMixedAlphaDigit() {
        assertThrows(Exception.class, () -> new CompanyIcaoCode("T1P"));
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final var ctor = CompanyIcaoCode.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var code = ctor.newInstance();
        assertNotNull(code);
    }
}
