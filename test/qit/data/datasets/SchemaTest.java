package qit.data.datasets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaTest
{
    // ===== construction =====

    @Test
    void constructEmpty()
    {
        Schema s = new Schema();
        assertEquals(0, s.size());
    }

    @Test
    void constructFromIterable()
    {
        Schema s = new Schema(Arrays.asList("x", "y", "z"));
        assertEquals(3, s.size());
        assertEquals("x", s.getField(0));
        assertEquals("y", s.getField(1));
        assertEquals("z", s.getField(2));
    }

    // ===== add / size =====

    @Test
    void addField()
    {
        Schema s = new Schema();
        s.add("name");
        s.add("age");
        assertEquals(2, s.size());
    }

    @Test
    void addFieldWithDefault()
    {
        Schema s = new Schema();
        s.add("color", "red");
        assertEquals("red", s.getDefault("color"));
        assertEquals("red", s.getDefault(0));
    }

    @Test
    void addDuplicateThrows()
    {
        Schema s = new Schema();
        s.add("name");
        assertThrows(RuntimeException.class, () -> s.add("name"));
    }

    // ===== hasField =====

    @Test
    void hasField()
    {
        Schema s = new Schema();
        s.add("name");
        assertTrue(s.hasField("name"));
        assertFalse(s.hasField("age"));
    }

    // ===== getIndex =====

    @Test
    void getIndex()
    {
        Schema s = new Schema();
        s.add("a");
        s.add("b");
        s.add("c");
        assertEquals(0, s.getIndex("a"));
        assertEquals(1, s.getIndex("b"));
        assertEquals(2, s.getIndex("c"));
    }

    @Test
    void getIndexMissingThrows()
    {
        Schema s = new Schema();
        s.add("a");
        assertThrows(RuntimeException.class, () -> s.getIndex("z"));
    }

    // ===== getField =====

    @Test
    void getField()
    {
        Schema s = new Schema();
        s.add("first");
        s.add("second");
        assertEquals("first", s.getField(0));
        assertEquals("second", s.getField(1));
    }

    // ===== getFields =====

    @Test
    void getFields()
    {
        Schema s = new Schema(Arrays.asList("a", "b", "c"));
        List<String> fields = s.getFields();
        assertEquals(3, fields.size());
        assertEquals("a", fields.get(0));
        assertEquals("c", fields.get(2));
    }

    // ===== lock =====

    @Test
    void lock()
    {
        Schema s = new Schema();
        assertFalse(s.locked());
        s.lock();
        assertTrue(s.locked());
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Schema s = new Schema();
        s.add("x");
        s.add("y");
        Schema c = s.copy();
        assertEquals(s.size(), c.size());
        assertEquals("x", c.getField(0));
        assertEquals("y", c.getField(1));
    }

    @Test
    void copyIndependence()
    {
        Schema s = new Schema();
        s.add("x");
        Schema c = s.copy();
        c.add("y");
        assertEquals(1, s.size());
        assertEquals(2, c.size());
    }

    // ===== iterator =====

    @Test
    void iterator()
    {
        Schema s = new Schema(Arrays.asList("a", "b", "c"));
        int count = 0;
        for (String field : s)
        {
            count++;
        }
        assertEquals(3, count);
    }

    // ===== defaults =====

    @Test
    void defaultIsNullWhenNotSpecified()
    {
        Schema s = new Schema();
        s.add("name");
        assertNull(s.getDefault("name"));
    }
}
