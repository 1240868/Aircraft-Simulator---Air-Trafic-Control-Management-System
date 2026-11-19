package eapli.aisafe.company.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class AirTransportCompanyCollaboratorTest {

    private static final CompanyName NAME = new CompanyName("TAP Air Portugal");
    private static final CompanyIataCode IATA = new CompanyIataCode("TP");
    private static final CompanyIcaoCode ICAO = new CompanyIcaoCode("TAP");
    private static final AirOperatorCertificate CERT =
            new AirOperatorCertificate("AOC-001", LocalDate.of(2030, 12, 31));

    private AirTransportCompany buildCompany() {
        return new AirTransportCompany(NAME, IATA, ICAO, CERT);
    }

    private AirTransportCompany buildViaOrm() throws Exception {
        final Constructor<AirTransportCompany> ctor =
                AirTransportCompany.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private void nullifyCollaborators(final AirTransportCompany company) throws Exception {
        final Field f = AirTransportCompany.class.getDeclaredField("collaborators");
        f.setAccessible(true);
        f.set(company, null);
    }

    private void setId(final AirTransportCompany company, final CompanyId id) throws Exception {
        final Field f = AirTransportCompany.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(company, id);
    }

    // ── constructor & identity ──────────────────────────────────────

    @Test
    public void constructorShouldAssignIdentity() {
        final AirTransportCompany company = buildCompany();
        assertNotNull(company.identity());
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldRejectNullName() {
        new AirTransportCompany(null, IATA, ICAO, CERT);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldRejectNullIata() {
        new AirTransportCompany(NAME, null, ICAO, CERT);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldRejectNullIcao() {
        new AirTransportCompany(NAME, IATA, null, CERT);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldRejectNullCertificate() {
        new AirTransportCompany(NAME, IATA, ICAO, null);
    }

    // ── getters ─────────────────────────────────────────────────────

    @Test
    public void nameShouldReturnCompanyName() {
        assertEquals(NAME, buildCompany().name());
    }

    @Test
    public void iataCodeShouldReturnValue() {
        assertEquals(IATA, buildCompany().iataCode());
    }

    @Test
    public void icaoCodeShouldReturnValue() {
        assertEquals(ICAO, buildCompany().icaoCode());
    }

    @Test
    public void certificateShouldReturnValue() {
        assertEquals(CERT, buildCompany().certificate());
    }

    // ── sameAs ──────────────────────────────────────────────────────

    @Test
    public void sameAsShouldReturnTrueForSameObject() {
        final AirTransportCompany company = buildCompany();
        assertTrue(company.sameAs(company));
    }

    @Test
    public void sameAsShouldReturnTrueForSameIdentityDifferentObject() throws Exception {
        final AirTransportCompany a = buildCompany();
        final AirTransportCompany b = buildCompany();
        setId(b, a.identity());
        assertTrue(a.sameAs(b));
    }

    @Test
    public void sameAsShouldReturnFalseForDifferentIdentity() {
        final AirTransportCompany a = buildCompany();
        final AirTransportCompany b = buildCompany();
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsShouldReturnFalseForNonCompanyObject() {
        assertFalse(buildCompany().sameAs("not a company"));
    }

    @Test
    public void sameAsShouldReturnFalseForNull() {
        assertFalse(buildCompany().sameAs(null));
    }

    // ── addCollaborator – happy path ────────────────────────────────

    @Test
    public void shouldAddCollaboratorSuccessfully() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("collab1");
        assertTrue(company.collaboratorUsernames().contains("collab1"));
        assertEquals(1, company.collaboratorUsernames().size());
    }

    @Test
    public void shouldAddMultipleCollaborators() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("collab1");
        company.addCollaborator("collab2");
        company.addCollaborator("collab3");
        assertEquals(3, company.collaboratorUsernames().size());
    }

    @Test
    public void shouldTrimUsername() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("  collab1  ");
        assertTrue(company.collaboratorUsernames().contains("collab1"));
    }

    // ── addCollaborator – invalid input ─────────────────────────────

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUsername() {
        buildCompany().addCollaborator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankUsername() {
        buildCompany().addCollaborator("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyUsername() {
        buildCompany().addCollaborator("");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectDuplicateCollaborator() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("collab1");
        company.addCollaborator("collab1");
    }

    // ── collaboratorUsernames / listActiveCollaborators ──────────────

    @Test
    public void collaboratorUsernamesShouldBeUnmodifiable() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("collab1");
        try {
            company.collaboratorUsernames().add("hacker");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void listActiveCollaboratorsShouldReturnAllAdded() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("collab1");
        company.addCollaborator("collab2");
        final List<String> list = company.listActiveCollaborators();
        assertEquals(2, list.size());
        assertTrue(list.contains("collab1"));
        assertTrue(list.contains("collab2"));
    }

    @Test
    public void emptyCompanyShouldHaveNoCollaborators() {
        final AirTransportCompany company = buildCompany();
        assertTrue(company.collaboratorUsernames().isEmpty());
        assertTrue(company.listActiveCollaborators().isEmpty());
    }

    // ── ORM constructor ─────────────────────────────────────────────

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        assertNotNull(buildViaOrm());
    }

    // ── ORM null-field paths (reflection) ───────────────────────────

    @Test
    public void collaboratorUsernamesShouldReturnEmptySetWhenFieldIsNull() throws Exception {
        final AirTransportCompany company = buildViaOrm();
        nullifyCollaborators(company);
        final Set<String> result = company.collaboratorUsernames();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listActiveCollaboratorsShouldReturnEmptyListWhenFieldIsNull() throws Exception {
        final AirTransportCompany company = buildViaOrm();
        nullifyCollaborators(company);
        final List<String> result = company.listActiveCollaborators();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void addCollaboratorShouldInitSetWhenFieldIsNull() throws Exception {
        final AirTransportCompany company = buildViaOrm();
        nullifyCollaborators(company);
        company.addCollaborator("ormUser");
        assertTrue(company.collaboratorUsernames().contains("ormUser"));
    }

    // ── ORM non-null path ───────────────────────────────────────────

    @Test
    public void collaboratorUsernamesShouldReturnValuesWhenFieldIsNotNull() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("user1");
        final Set<String> result = company.collaboratorUsernames();
        assertEquals(1, result.size());
        assertTrue(result.contains("user1"));
    }

    @Test
    public void listActiveCollaboratorsShouldReturnValuesWhenFieldIsNotNull() {
        final AirTransportCompany company = buildCompany();
        company.addCollaborator("user1");
        final List<String> result = company.listActiveCollaborators();
        assertEquals(1, result.size());
        assertTrue(result.contains("user1"));
    }
}
