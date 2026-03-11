package qit.data.datasets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest
{
    // ===== construction =====

    @Test
    void constructEmpty()
    {
        Record r = new Record();
        assertEquals(0, r.size());
    }

    // ===== with =====

    @Test
    void withStringValue()
    {
        Record r = new Record().with("name", "alice");
        assertEquals("alice", r.get("name"));
        assertEquals(1, r.size());
    }

    @Test
    void withChaining()
    {
        Record r = new Record()
            .with("name", "alice")
            .with("age", "30")
            .with("city", "nyc");
        assertEquals(3, r.size());
        assertEquals("alice", r.get("name"));
        assertEquals("30", r.get("age"));
        assertEquals("nyc", r.get("city"));
    }

    @Test
    void withObjectValue()
    {
        Record r = new Record().with("count", 42);
        assertEquals("42", r.get("count"));
    }

    @Test
    void withRecord()
    {
        Record a = new Record().with("x", "1").with("y", "2");
        Record b = new Record().with("z", "3").with(a);
        assertEquals(3, b.size());
        assertEquals("1", b.get("x"));
        assertEquals("2", b.get("y"));
        assertEquals("3", b.get("z"));
    }

    @Test
    void withNullKeyThrows()
    {
        assertThrows(RuntimeException.class,
            () -> new Record().with(null, "value"));
    }

    // ===== get =====

    @Test
    void getMissingReturnsNull()
    {
        Record r = new Record();
        assertNull(r.get("missing"));
    }

    // ===== containsKey =====

    @Test
    void containsKey()
    {
        Record r = new Record().with("name", "test");
        assertTrue(r.containsKey("name"));
        assertFalse(r.containsKey("missing"));
    }

    // ===== remove =====

    @Test
    void remove()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        String removed = r.remove("a");
        assertEquals("1", removed);
        assertEquals(1, r.size());
        assertFalse(r.containsKey("a"));
    }

    @Test
    void removeMissing()
    {
        Record r = new Record().with("a", "1");
        assertNull(r.remove("missing"));
        assertEquals(1, r.size());
    }

    // ===== clear =====

    @Test
    void clear()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        r.clear();
        assertEquals(0, r.size());
    }

    // ===== keys =====

    @Test
    void keySet()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        Set<String> keys = r.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
    }

    @Test
    void keysList()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        List<String> keys = r.keys();
        assertEquals(2, keys.size());
    }

    // ===== select =====

    @Test
    void selectSet()
    {
        Record r = new Record().with("a", "1").with("b", "2").with("c", "3");
        Set<String> keep = new HashSet<>(Arrays.asList("a", "c"));
        Record selected = r.select(keep);
        assertEquals(2, selected.size());
        assertEquals("1", selected.get("a"));
        assertEquals("3", selected.get("c"));
        assertNull(selected.get("b"));
    }

    @Test
    void selectList()
    {
        Record r = new Record().with("a", "1").with("b", "2").with("c", "3");
        Record selected = r.select(Arrays.asList("b"));
        assertEquals(1, selected.size());
        assertEquals("2", selected.get("b"));
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        Record c = r.copy();
        assertEquals(r.size(), c.size());
        assertEquals(r.get("a"), c.get("a"));
        assertEquals(r.get("b"), c.get("b"));
    }

    @Test
    void copyIndependence()
    {
        Record r = new Record().with("a", "1");
        Record c = r.copy();
        c.with("b", "2");
        assertEquals(1, r.size());
        assertEquals(2, c.size());
    }

    // ===== equals / hashCode =====

    @Test
    void equalsIdentical()
    {
        Record a = new Record().with("x", "1").with("y", "2");
        Record b = new Record().with("x", "1").with("y", "2");
        assertEquals(a, b);
    }

    @Test
    void equalsDifferentValues()
    {
        Record a = new Record().with("x", "1");
        Record b = new Record().with("x", "2");
        assertNotEquals(a, b);
    }

    @Test
    void equalsDifferentKeys()
    {
        Record a = new Record().with("x", "1");
        Record b = new Record().with("y", "1");
        assertNotEquals(a, b);
    }

    @Test
    void hashCodeConsistent()
    {
        Record a = new Record().with("x", "1");
        Record b = new Record().with("x", "1");
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== iterator =====

    @Test
    void iterator()
    {
        Record r = new Record().with("a", "1").with("b", "2").with("c", "3");
        int count = 0;
        for (String key : r)
        {
            assertNotNull(r.get(key));
            count++;
        }
        assertEquals(3, count);
    }

    // ===== toString =====

    @Test
    void toStringNotNull()
    {
        Record r = new Record().with("a", "1");
        assertNotNull(r.toString());
    }

    @Test
    void toStringFlatNotNull()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        String flat = r.toStringFlat();
        assertNotNull(flat);
        assertTrue(flat.contains("a"));
        assertTrue(flat.contains("1"));
    }

    // ===== map =====

    @Test
    void map()
    {
        Record r = new Record().with("a", "1").with("b", "2");
        assertEquals(2, r.map().size());
        assertEquals("1", r.map().get("a"));
    }

    // ===== insertion order =====

    @Test
    void preservesInsertionOrder()
    {
        Record r = new Record()
            .with("z", "3")
            .with("a", "1")
            .with("m", "2");
        List<String> keys = r.keys();
        assertEquals("z", keys.get(0));
        assertEquals("a", keys.get(1));
        assertEquals("m", keys.get(2));
    }
}
