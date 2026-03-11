package qit.base.cli;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CliValuesTest
{
    // ===== construction =====

    @Test
    void constructDefault()
    {
        CliValues values = new CliValues();
        assertNotNull(values);
    }

    // ===== positional args =====

    @Test
    void positionalArgs()
    {
        CliValues values = new CliValues();
        values.pos = new ArrayList<>(Arrays.asList("arg1", "arg2", "arg3"));
        assertEquals(3, values.pos.size());
        assertEquals("arg1", values.pos.get(0));
        assertEquals("arg2", values.pos.get(1));
        assertEquals("arg3", values.pos.get(2));
    }

    // ===== keyed args =====

    @Test
    void keyedArgs()
    {
        CliValues values = new CliValues();
        values.keyed = new HashMap<>();
        values.keyed.put("--input", Arrays.asList("file.nii.gz"));
        values.keyed.put("--threshold", Arrays.asList("0.5"));

        assertTrue(values.keyed.containsKey("--input"));
        assertEquals("file.nii.gz", values.keyed.get("--input").get(0));
        assertEquals("0.5", values.keyed.get("--threshold").get(0));
    }

    @Test
    void keyedMultipleValues()
    {
        CliValues values = new CliValues();
        values.keyed = new HashMap<>();
        values.keyed.put("--labels", Arrays.asList("1", "2", "3"));

        List<String> labels = values.keyed.get("--labels");
        assertEquals(3, labels.size());
    }

    // ===== integration with CliSpecification =====

    @Test
    void parseThroughSpec()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("input")
                .withArg("<String>")
                .withNum(1)
                .asInput())
            .withOption(new CliOption()
                .withName("output")
                .withArg("<String>")
                .withNum(1)
                .asOutput());

        CliValues values = spec.parse(new String[]{
            "--input", "in.txt",
            "--output", "out.txt"
        });

        assertEquals("in.txt", values.keyed.get("input").get(0));
        assertEquals("out.txt", values.keyed.get("output").get(0));
    }
}
