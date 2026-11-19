package eapli.aisafe.company.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class CompanyIdTest {

    @Test
    public void shouldCreateFromUuid() {
        final var uuid = UUID.randomUUID();
        final var id = new CompanyId(uuid);
        assertNotNull(id);
    }

    @Test
    public void newIdShouldGenerateNonNullId() {
        final var id = CompanyId.newId();
        assertNotNull(id);
        assertNotNull(id.asUuid());
    }

    @Test
    public void asUuidShouldReturnOriginalUuid() {
        final var uuid = UUID.randomUUID();
        final var id = new CompanyId(uuid);
        assertEquals(uuid, id.asUuid());
    }

    @Test
    public void shouldRejectNullUuid() {
        assertThrows(NullPointerException.class, () -> new CompanyId(null));
    }

    @Test
    public void equalsShouldReturnTrueForSameUuid() {
        final var uuid = UUID.randomUUID();
        final var a = new CompanyId(uuid);
        final var b = new CompanyId(uuid);
        assertEquals(a, b);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentUuid() {
        final var a = CompanyId.newId();
        final var b = CompanyId.newId();
        assertNotEquals(a, b);
    }

    @Test
    public void equalsShouldReturnTrueForSelf() {
        final var id = CompanyId.newId();
        assertEquals(id, id);
    }

    @Test
    public void equalsShouldReturnFalseForNull() {
        final var id = CompanyId.newId();
        assertFalse(id.equals(null));
    }

    @Test
    public void equalsShouldReturnFalseForDifferentType() {
        final var id = CompanyId.newId();
        assertFalse(id.equals("not-a-company-id"));
    }

    @Test
    public void hashCodeShouldBeConsistentWithEquals() {
        final var uuid = UUID.randomUUID();
        final var a = new CompanyId(uuid);
        final var b = new CompanyId(uuid);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void compareToShouldReturnZeroForEqualIds() {
        final var uuid = UUID.randomUUID();
        final var a = new CompanyId(uuid);
        final var b = new CompanyId(uuid);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void compareToShouldReturnNegativeForLesser() {
        final var a = new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        final var b = new CompanyId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    public void compareToShouldReturnPositiveForGreater() {
        final var a = new CompanyId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        final var b = new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertTrue(a.compareTo(b) > 0);
    }

    @Test
    public void toStringShouldReturnUuidString() {
        final var uuid = UUID.randomUUID();
        final var id = new CompanyId(uuid);
        assertEquals(uuid.toString(), id.toString());
    }

    @Test
    public void ormConstructorShouldBeInstantiable() throws Exception {
        final var ctor = CompanyId.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final var id = ctor.newInstance();
        assertNotNull(id);
    }
}
