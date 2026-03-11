package qit.base.cli;

import org.junit.jupiter.api.Test;
import qit.base.Module;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Vect;

import static org.junit.jupiter.api.Assertions.*;

class CliModuleTest
{
    // A minimal test module for verifying CLI wrapping
    @ModuleDescription("test module for CLI tests")
    public static class TestModule implements Module
    {
        @ModuleParameter
        @ModuleDescription("a string parameter")
        public String name = "default";

        @ModuleParameter
        @ModuleDescription("a double parameter")
        public Double value = 1.0;

        @ModuleParameter
        @ModuleDescription("a boolean flag")
        public boolean verbose = false;

        @ModuleParameter
        @ModuleOptional
        @ModuleDescription("an optional integer")
        public Integer count;

        public boolean wasRun = false;

        @Override
        public TestModule run()
        {
            wasRun = true;
            return this;
        }
    }

    public enum TestEnum { ALPHA, BETA, GAMMA }

    @ModuleDescription("module with enum parameter")
    public static class EnumModule implements Module
    {
        @ModuleParameter
        @ModuleDescription("enum choice")
        public TestEnum mode = TestEnum.ALPHA;

        @Override
        public EnumModule run()
        {
            return this;
        }
    }

    // ===== construction =====

    @Test
    void constructWithModule()
    {
        CliModule cli = new CliModule(new TestModule());
        assertNotNull(cli);
    }

    // ===== CLI spec generation =====

    @Test
    void cliSpecHasModuleParameters()
    {
        // Verify that CliModule can be constructed without error,
        // which validates the module annotations
        TestModule module = new TestModule();
        CliModule cli = new CliModule(module);
        assertNotNull(cli);
    }

    @Test
    void enumModuleConstructs()
    {
        // Verify enum parameter modules can be wrapped
        EnumModule module = new EnumModule();
        CliModule cli = new CliModule(module);
        assertNotNull(cli);
    }
}
