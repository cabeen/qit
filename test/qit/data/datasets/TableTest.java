package qit.data.datasets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableTest
{
    // ===== construction =====

    @Test
    void constructEmpty()
    {
        Table t = new Table();
        assertEquals(0, t.getNumRecords());
        assertEquals(0, t.getNumFields());
    }

    @Test
    void constructFromSchema()
    {
        Schema s = new Schema(Arrays.asList("name", "age"));
        Table t = new Table(s);
        assertEquals(0, t.getNumRecords());
        assertEquals(2, t.getNumFields());
        assertTrue(t.hasField("name"));
        assertTrue(t.hasField("age"));
    }

    @Test
    void constructFromIterable()
    {
        Table t = new Table(Arrays.asList("x", "y", "z"));
        assertEquals(3, t.getNumFields());
    }

    // ===== addField =====

    @Test
    void addField()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        assertEquals(2, t.getNumFields());
        assertTrue(t.hasField("name"));
        assertTrue(t.hasField("value"));
    }

    @Test
    void addFieldIdempotent()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("name"); // withField skips duplicates
        assertEquals(1, t.getNumFields());
    }

    @Test
    void addFields()
    {
        Table t = new Table();
        t.addFields(Arrays.asList("a", "b", "c"));
        assertEquals(3, t.getNumFields());
    }

    // ===== addRecord =====

    @Test
    void addRecordFromArray()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        t.addRecord(new String[]{"foo", "42"});
        assertEquals(1, t.getNumRecords());
    }

    @Test
    void addRecordFromRecord()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        Record r = new Record().with("name", "bar").with("value", "99");
        t.addRecord(r);
        assertEquals(1, t.getNumRecords());
    }

    @Test
    void addMultipleRecords()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        t.addRecord(new String[]{"2"});
        t.addRecord(new String[]{"3"});
        assertEquals(3, t.getNumRecords());
    }

    // ===== get / set =====

    @Test
    void getByKeyAndField()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        Record r = new Record().with("name", "test").with("value", "123");
        t.addRecord(r);

        List<Integer> keys = t.getKeys();
        assertEquals(1, keys.size());
        assertEquals("test", t.get(keys.get(0), "name"));
        assertEquals("123", t.get(keys.get(0), "value"));
    }

    @Test
    void setByKeyAndField()
    {
        Table t = new Table();
        t.addField("name");
        t.set(1, "name", "hello");
        assertEquals("hello", t.get(1, "name"));
    }

    @Test
    void setCreatesFieldIfMissing()
    {
        Table t = new Table();
        t.set(1, "newfield", "val");
        assertTrue(t.hasField("newfield"));
        assertEquals("val", t.get(1, "newfield"));
    }

    // ===== field access =====

    @Test
    void getFields()
    {
        Table t = new Table();
        t.addField("a");
        t.addField("b");
        List<String> fields = t.getFields();
        assertEquals(2, fields.size());
        assertEquals("a", fields.get(0));
        assertEquals("b", fields.get(1));
    }

    @Test
    void getFieldName()
    {
        Table t = new Table();
        t.addField("first");
        t.addField("second");
        assertEquals("first", t.getFieldName(0));
        assertEquals("second", t.getFieldName(1));
    }

    @Test
    void getFieldIndex()
    {
        Table t = new Table();
        t.addField("a");
        t.addField("b");
        assertEquals(0, t.getFieldIndex("a"));
        assertEquals(1, t.getFieldIndex("b"));
    }

    // ===== getRecord =====

    @Test
    void getRecord()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        Record r = new Record().with("name", "foo").with("value", "bar");
        t.addRecord(r);

        Integer key = t.getKeys().get(0);
        Record retrieved = t.getRecord(key);
        assertEquals("foo", retrieved.get("name"));
        assertEquals("bar", retrieved.get("value"));
    }

    @Test
    void getRecords()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        t.addRecord(new String[]{"2"});
        List<Record> records = t.getRecords();
        assertEquals(2, records.size());
    }

    // ===== getFieldValues =====

    @Test
    void getFieldValues()
    {
        Table t = new Table();
        t.addField("name");
        t.addRecord(new String[]{"alice"});
        t.addRecord(new String[]{"bob"});
        t.addRecord(new String[]{"charlie"});

        List<String> values = t.getFieldValues("name");
        assertEquals(3, values.size());
        assertTrue(values.contains("alice"));
        assertTrue(values.contains("bob"));
        assertTrue(values.contains("charlie"));
    }

    // ===== remove =====

    @Test
    void remove()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        t.addRecord(new String[]{"2"});
        assertEquals(2, t.getNumRecords());

        Integer key = t.getKeys().get(0);
        t.remove(key);
        assertEquals(1, t.getNumRecords());
    }

    // ===== hasRow =====

    @Test
    void hasRow()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        Integer key = t.getKeys().get(0);
        assertTrue(t.hasRow(key));
        assertFalse(t.hasRow(999));
    }

    // ===== where =====

    @Test
    void where()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("color");
        t.addRecord(new Record().with("name", "a").with("color", "red"));
        t.addRecord(new Record().with("name", "b").with("color", "blue"));
        t.addRecord(new Record().with("name", "c").with("color", "red"));

        Record query = new Record().with("color", "red");
        List<Record> results = t.where(query);
        assertEquals(2, results.size());
    }

    // ===== withField with defaults =====

    @Test
    void withFieldDefault()
    {
        Table t = new Table();
        t.addField("name");
        t.addRecord(new String[]{"alice"});

        // adding a new field with default should expand existing rows
        t.withField("status", "active");
        assertEquals(2, t.getNumFields());
        Integer key = t.getKeys().get(0);
        assertEquals("active", t.get(key, "status"));
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        t.addRecord(new String[]{"2"});

        Table c = t.copy();
        assertEquals(t.getNumRecords(), c.getNumRecords());
        assertEquals(t.getNumFields(), c.getNumFields());
    }

    @Test
    void copyIndependence()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});

        Table c = t.copy();
        c.addRecord(new String[]{"2"});
        assertEquals(1, t.getNumRecords());
        assertEquals(2, c.getNumRecords());
    }

    // ===== proto =====

    @Test
    void proto()
    {
        Table t = new Table();
        t.addField("a");
        t.addField("b");
        t.addRecord(new String[]{"1", "2"});

        Table p = t.proto();
        assertEquals(2, p.getNumFields());
        assertEquals(0, p.getNumRecords());
    }

    // ===== schema =====

    @Test
    void getSchema()
    {
        Table t = new Table();
        t.addField("name");
        t.addField("value");
        Schema s = t.getSchema();
        assertEquals(2, s.size());
        assertTrue(s.hasField("name"));
        assertTrue(s.locked()); // getSchema returns a locked copy
    }

    // ===== iterator =====

    @Test
    void iterator()
    {
        Table t = new Table();
        t.addField("x");
        t.addRecord(new String[]{"1"});
        t.addRecord(new String[]{"2"});
        t.addRecord(new String[]{"3"});

        int count = 0;
        for (String[] row : t)
        {
            count++;
        }
        assertEquals(3, count);
    }
}
