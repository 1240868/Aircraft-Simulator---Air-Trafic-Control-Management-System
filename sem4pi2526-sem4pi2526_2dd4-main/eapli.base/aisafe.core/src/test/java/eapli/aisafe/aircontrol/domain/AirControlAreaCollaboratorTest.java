package eapli.aisafe.aircontrol.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class AirControlAreaCollaboratorTest {

    private AirControlArea buildArea() {
        return new AirControlArea("PT-LIS", "Lisboa FIR",
                36.0, 42.0, -10.0, -6.0);
    }

    private AirControlArea buildViaOrm() throws Exception {
        final Constructor<AirControlArea> ctor =
                AirControlArea.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private void nullifyCollaborators(final AirControlArea area) throws Exception {
        final Field f = AirControlArea.class.getDeclaredField("collaborators");
        f.setAccessible(true);
        f.set(area, null);
    }

    // ── constructor – happy path ────────────────────────────────────

    @Test
    public void constructorShouldCreateValidArea() {
        final AirControlArea area = buildArea();
        assertNotNull(area.identity());
        assertEquals(AirControlAreaCode.valueOf("PT-LIS"), area.identity());
    }

    @Test
    public void shouldExposeAllGetters() {
        final AirControlArea area = buildArea();
        assertEquals("Lisboa FIR", area.name());
        assertEquals(AirControlAreaCode.valueOf("PT-LIS"), area.areaCode());
        assertEquals(36.0, area.latMin(), 0.0001);
        assertEquals(42.0, area.latMax(), 0.0001);
        assertEquals(-10.0, area.lonMin(), 0.0001);
        assertEquals(-6.0, area.lonMax(), 0.0001);
    }

    // ── constructor – name validation ───────────────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectNullName() {
        new AirControlArea("X", null, 0, 1, 0, 1);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectEmptyName() {
        new AirControlArea("X", "", 0, 1, 0, 1);
    }

    // ── constructor – latMin both sides of && ───────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMinBelowMinus90() {
        new AirControlArea("X", "Area", -91, 42, -10, -6);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMinAbove90() {
        new AirControlArea("X", "Area", 91, 92, -10, -6);
    }

    // ── constructor – latMax both sides of && ───────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMaxBelowMinus90() {
        new AirControlArea("X", "Area", -90, -91, -10, -6);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMaxAbove90() {
        new AirControlArea("X", "Area", 36, 91, -10, -6);
    }

    // ── constructor – latMin < latMax ───────────────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMinGreaterThanLatMax() {
        new AirControlArea("X", "Area", 42, 36, -10, -6);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLatMinEqualToLatMax() {
        new AirControlArea("X", "Area", 40, 40, -10, -6);
    }

    // ── constructor – lonMin both sides of && ───────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMinBelowMinus180() {
        new AirControlArea("X", "Area", 36, 42, -181, -6);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMinAbove180() {
        new AirControlArea("X", "Area", 36, 42, 181, 182);
    }

    // ── constructor – lonMax both sides of && ───────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMaxBelowMinus180() {
        new AirControlArea("X", "Area", 36, 42, -180, -181);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMaxAbove180() {
        new AirControlArea("X", "Area", 36, 42, -10, 181);
    }

    // ── constructor – lonMin < lonMax ───────────────────────────────

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMinGreaterThanLonMax() {
        new AirControlArea("X", "Area", 36, 42, -6, -10);
    }

    @Test(expected = Exception.class)
    public void constructorShouldRejectLonMinEqualToLonMax() {
        new AirControlArea("X", "Area", 36, 42, 5, 5);
    }

    // ── sameAs ──────────────────────────────────────────────────────

    @Test
    public void sameAsShouldReturnTrueForSameObject() {
        final AirControlArea area = buildArea();
        assertTrue(area.sameAs(area));
    }

    @Test
    public void sameAsShouldReturnTrueForSameIdentityDifferentObject() {
        final AirControlArea a = new AirControlArea("PT-LIS", "Lisboa FIR", 36, 42, -10, -6);
        final AirControlArea b = new AirControlArea("PT-LIS", "Other Name", 37, 43, -9, -5);
        assertTrue(a.sameAs(b));
    }

    @Test
    public void sameAsShouldReturnFalseForDifferentIdentity() {
        final AirControlArea a = buildArea();
        final AirControlArea b = new AirControlArea("PT-OPO", "Porto FIR", 40, 43, -9, -7);
        assertFalse(a.sameAs(b));
    }

    @Test
    public void sameAsShouldReturnFalseForNonAreaObject() {
        assertFalse(buildArea().sameAs("not an area"));
    }

    @Test
    public void sameAsShouldReturnFalseForNull() {
        assertFalse(buildArea().sameAs(null));
    }

    // ── toString ────────────────────────────────────────────────────

    @Test
    public void toStringShouldContainCodeAndName() {
        final String str = buildArea().toString();
        assertTrue(str.contains("PT-LIS"));
        assertTrue(str.contains("Lisboa FIR"));
    }

    // ── addCollaborator – happy path ────────────────────────────────

    @Test
    public void shouldAddCollaboratorSuccessfully() {
        final AirControlArea area = buildArea();
        area.addCollaborator("collab1");
        assertTrue(area.collaboratorUsernames().contains("collab1"));
        assertEquals(1, area.collaboratorUsernames().size());
    }

    @Test
    public void shouldAddMultipleCollaborators() {
        final AirControlArea area = buildArea();
        area.addCollaborator("collab1");
        area.addCollaborator("collab2");
        area.addCollaborator("collab3");
        assertEquals(3, area.collaboratorUsernames().size());
    }

    @Test
    public void shouldTrimUsername() {
        final AirControlArea area = buildArea();
        area.addCollaborator("  collab1  ");
        assertTrue(area.collaboratorUsernames().contains("collab1"));
    }

    // ── addCollaborator – invalid input ─────────────────────────────

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullUsername() {
        buildArea().addCollaborator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBlankUsername() {
        buildArea().addCollaborator("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyUsername() {
        buildArea().addCollaborator("");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectDuplicateCollaborator() {
        final AirControlArea area = buildArea();
        area.addCollaborator("collab1");
        area.addCollaborator("collab1");
    }

    // ── collaboratorUsernames / listActiveCollaborators ──────────────

    @Test
    public void collaboratorUsernamesShouldBeUnmodifiable() {
        final AirControlArea area = buildArea();
        area.addCollaborator("collab1");
        try {
            area.collaboratorUsernames().add("hacker");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void listActiveCollaboratorsShouldReturnAllAdded() {
        final AirControlArea area = buildArea();
        area.addCollaborator("collab1");
        area.addCollaborator("collab2");
        final List<String> list = area.listActiveCollaborators();
        assertEquals(2, list.size());
        assertTrue(list.contains("collab1"));
        assertTrue(list.contains("collab2"));
    }

    @Test
    public void emptyAreaShouldHaveNoCollaborators() {
        final AirControlArea area = buildArea();
        assertTrue(area.collaboratorUsernames().isEmpty());
        assertTrue(area.listActiveCollaborators().isEmpty());
    }

    // ── ORM constructor ─────────────────────────────────────────────

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        assertNotNull(buildViaOrm());
    }

    // ── ORM null-field paths (reflection) ───────────────────────────

    @Test
    public void collaboratorUsernamesShouldReturnEmptySetWhenFieldIsNull() throws Exception {
        final AirControlArea area = buildViaOrm();
        nullifyCollaborators(area);
        final Set<String> result = area.collaboratorUsernames();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listActiveCollaboratorsShouldReturnEmptyListWhenFieldIsNull() throws Exception {
        final AirControlArea area = buildViaOrm();
        nullifyCollaborators(area);
        final List<String> result = area.listActiveCollaborators();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void addCollaboratorShouldInitSetWhenFieldIsNull() throws Exception {
        final AirControlArea area = buildViaOrm();
        nullifyCollaborators(area);
        area.addCollaborator("ormUser");
        assertTrue(area.collaboratorUsernames().contains("ormUser"));
    }

    // ── ORM non-null path ───────────────────────────────────────────

    @Test
    public void collaboratorUsernamesShouldReturnValuesWhenFieldIsNotNull() {
        final AirControlArea area = buildArea();
        area.addCollaborator("user1");
        final Set<String> result = area.collaboratorUsernames();
        assertEquals(1, result.size());
        assertTrue(result.contains("user1"));
    }

    @Test
    public void listActiveCollaboratorsShouldReturnValuesWhenFieldIsNotNull() {
        final AirControlArea area = buildArea();
        area.addCollaborator("user1");
        final List<String> result = area.listActiveCollaborators();
        assertEquals(1, result.size());
        assertTrue(result.contains("user1"));
    }
}
