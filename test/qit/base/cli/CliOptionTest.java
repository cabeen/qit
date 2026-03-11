package qit.base.cli;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CliOptionTest
{
    // ===== construction =====

    @Test
    void constructDefault()
    {
        CliOption opt = new CliOption();
        assertNotNull(opt.getName());
        assertNotNull(opt.getDoc());
    }

    // ===== builder pattern =====

    @Test
    void withName()
    {
        CliOption opt = new CliOption().withName("--input");
        assertEquals("--input", opt.getName());
    }

    @Test
    void withDoc()
    {
        CliOption opt = new CliOption().withDoc("input volume");
        assertEquals("input volume", opt.getDoc());
    }

    @Test
    void withArg()
    {
        CliOption opt = new CliOption().withArg("<Volume>");
        assertEquals(1, opt.getArgs().size());
        assertEquals("<Volume>", opt.getArgs().get(0));
    }

    @Test
    void withArgs()
    {
        CliOption opt = new CliOption().withArgs(Arrays.asList("<x>", "<y>", "<z>"));
        assertEquals(3, opt.getArgs().size());
    }

    @Test
    void withDefault()
    {
        CliOption opt = new CliOption().withDefault("0.5");
        assertTrue(opt.hasDefault());
        assertEquals("0.5", opt.getDefault());
    }

    @Test
    void noDefaultByDefault()
    {
        CliOption opt = new CliOption();
        assertFalse(opt.hasDefault());
    }

    @Test
    void withMinMax()
    {
        CliOption opt = new CliOption().withMin(1).withMax(3);
        assertEquals(1, opt.getMin());
        assertEquals(3, opt.getMax());
    }

    @Test
    void withNum()
    {
        CliOption opt = new CliOption().withNum(2);
        assertEquals(2, opt.getMin());
        assertEquals(2, opt.getMax());
    }

    @Test
    void withNoMax()
    {
        CliOption opt = new CliOption().withNoMax();
        assertEquals(Integer.MAX_VALUE, opt.getMax());
    }

    // ===== type flags =====

    @Test
    void asInput()
    {
        CliOption opt = new CliOption().asInput();
        assertTrue(opt.isInput());
        assertFalse(opt.isParameter());
        assertFalse(opt.isOutput());
    }

    @Test
    void asParameter()
    {
        CliOption opt = new CliOption().asParameter();
        assertFalse(opt.isInput());
        assertTrue(opt.isParameter());
        assertFalse(opt.isOutput());
    }

    @Test
    void asOutput()
    {
        CliOption opt = new CliOption().asOutput();
        assertFalse(opt.isInput());
        assertFalse(opt.isParameter());
        assertTrue(opt.isOutput());
    }

    // ===== optional / advanced =====

    @Test
    void asOptional()
    {
        CliOption opt = new CliOption().asOptional();
        assertTrue(opt.isOptional());
    }

    @Test
    void notOptionalByDefault()
    {
        CliOption opt = new CliOption();
        assertFalse(opt.isOptional());
    }

    @Test
    void asAdvanced()
    {
        CliOption opt = new CliOption().asAdvanced();
        assertTrue(opt.isAdvanced());
    }

    @Test
    void notAdvancedByDefault()
    {
        CliOption opt = new CliOption();
        assertFalse(opt.isAdvanced());
    }

    // ===== chaining =====

    @Test
    void fullChain()
    {
        CliOption opt = new CliOption()
            .withName("--threshold")
            .withDoc("threshold value")
            .withArg("<Double>")
            .withDefault("0.5")
            .asParameter()
            .asOptional();

        assertEquals("--threshold", opt.getName());
        assertEquals("threshold value", opt.getDoc());
        assertEquals(1, opt.getArgs().size());
        assertEquals("0.5", opt.getDefault());
        assertTrue(opt.isParameter());
        assertTrue(opt.isOptional());
    }
}
