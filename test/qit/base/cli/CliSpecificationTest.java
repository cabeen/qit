package qit.base.cli;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliSpecificationTest
{
    // Note: CliSpecification stores option names WITHOUT the "--" prefix.
    // When parsing "--input foo", the key stored is "input".

    private CliSpecification basicSpec()
    {
        return new CliSpecification()
            .withName("TestModule")
            .withDoc("a test module")
            .withOption(new CliOption()
                .withName("input")
                .withDoc("input file")
                .withArg("<Volume>")
                .withNum(1)
                .asInput())
            .withOption(new CliOption()
                .withName("threshold")
                .withDoc("threshold value")
                .withArg("<Double>")
                .withNum(1)
                .withDefault("0.5")
                .asParameter()
                .asOptional())
            .withOption(new CliOption()
                .withName("output")
                .withDoc("output file")
                .withArg("<Mask>")
                .withNum(1)
                .asOutput());
    }

    // ===== construction =====

    @Test
    void constructEmpty()
    {
        CliSpecification spec = new CliSpecification();
        assertNotNull(spec);
    }

    @Test
    void constructWithMetadata()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withDoc("description")
            .withAuthor("Author")
            .withCitation("Citation");
        assertNotNull(spec);
    }

    // ===== filtering =====

    @Test
    void getInputRequired()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> inputs = spec.getInput(false);
        assertEquals(1, inputs.size());
        assertEquals("input", inputs.get(0).getName());
    }

    @Test
    void getInputOptional()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> inputs = spec.getInput(true);
        assertEquals(0, inputs.size());
    }

    @Test
    void getParameterOptional()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> params = spec.getParameter(true);
        assertEquals(1, params.size());
        assertEquals("threshold", params.get(0).getName());
    }

    @Test
    void getParameterRequired()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> params = spec.getParameter(false);
        assertEquals(0, params.size());
    }

    @Test
    void getOutputRequired()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> outputs = spec.getOutput(false);
        assertEquals(1, outputs.size());
        assertEquals("output", outputs.get(0).getName());
    }

    @Test
    void getOutputOptional()
    {
        CliSpecification spec = basicSpec();
        List<CliOption> outputs = spec.getOutput(true);
        assertEquals(0, outputs.size());
    }

    // ===== parse =====

    @Test
    void parseKeyedArgs()
    {
        CliSpecification spec = basicSpec();
        CliValues values = spec.parse(new String[]{
            "--input", "in.nii.gz",
            "--output", "out.nii.gz"
        });

        assertNotNull(values);
        assertNotNull(values.keyed);
        assertTrue(values.keyed.containsKey("input"));
        assertEquals("in.nii.gz", values.keyed.get("input").get(0));
        assertTrue(values.keyed.containsKey("output"));
        assertEquals("out.nii.gz", values.keyed.get("output").get(0));
    }

    @Test
    void parseWithDefault()
    {
        CliSpecification spec = basicSpec();
        CliValues values = spec.parse(new String[]{
            "--input", "in.nii.gz",
            "--output", "out.nii.gz"
        });

        assertTrue(values.keyed.containsKey("threshold"));
        assertEquals("0.5", values.keyed.get("threshold").get(0));
    }

    @Test
    void parseOverridesDefault()
    {
        CliSpecification spec = basicSpec();
        CliValues values = spec.parse(new String[]{
            "--input", "in.nii.gz",
            "--threshold", "0.8",
            "--output", "out.nii.gz"
        });

        assertEquals("0.8", values.keyed.get("threshold").get(0));
    }

    @Test
    void parseFromList()
    {
        CliSpecification spec = basicSpec();
        CliValues values = spec.parse(Arrays.asList(
            "--input", "in.nii.gz",
            "--output", "out.nii.gz"
        ));

        assertNotNull(values);
        assertTrue(values.keyed.containsKey("input"));
    }

    // ===== boolean flag =====

    @Test
    void parseBooleanFlag()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("flag")
                .withDoc("a boolean flag")
                .withNum(0)
                .asParameter()
                .asOptional());

        CliValues values = spec.parse(new String[]{"--flag"});
        assertTrue(values.keyed.containsKey("flag"));
    }

    // ===== advanced params =====

    @Test
    void getParameterAdvanced()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("normal")
                .withArg("<String>")
                .withNum(1)
                .asParameter()
                .asOptional())
            .withOption(new CliOption()
                .withName("advparam")
                .withArg("<String>")
                .withNum(1)
                .asParameter()
                .asOptional()
                .asAdvanced());

        List<CliOption> advanced = spec.getParameterAdvanced();
        assertEquals(1, advanced.size());
        assertEquals("advparam", advanced.get(0).getName());
    }

    // ===== multiple options =====

    @Test
    void multipleInputs()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("input1")
                .withArg("<Volume>")
                .withNum(1)
                .asInput())
            .withOption(new CliOption()
                .withName("input2")
                .withArg("<Mask>")
                .withNum(1)
                .asInput()
                .asOptional());

        List<CliOption> required = spec.getInput(false);
        List<CliOption> optional = spec.getInput(true);
        assertEquals(1, required.size());
        assertEquals(1, optional.size());
    }

    // ===== all optional spec =====

    @Test
    void parseAllOptional()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("name")
                .withArg("<String>")
                .withNum(1)
                .asParameter()
                .asOptional()
                .withDefault("world"));

        CliValues values = spec.parse(new String[]{});
        assertEquals("world", values.keyed.get("name").get(0));
    }

    @Test
    void parseAllOptionalOverridden()
    {
        CliSpecification spec = new CliSpecification()
            .withName("Test")
            .withOption(new CliOption()
                .withName("name")
                .withArg("<String>")
                .withNum(1)
                .asParameter()
                .asOptional()
                .withDefault("world"));

        CliValues values = spec.parse(new String[]{"--name", "hello"});
        assertEquals("hello", values.keyed.get("name").get(0));
    }
}
