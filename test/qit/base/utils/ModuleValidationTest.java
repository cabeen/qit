package qit.base.utils;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import qit.base.Module;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ModuleValidationTest
{
    @Test
    void moduleDiscoveryFindsModules()
    {
        Map<String, Class<? extends Module>> modules = ModuleUtils.listedClasses();
        assertTrue(modules.size() > 100,
            "Expected 100+ listed modules, found " + modules.size());
    }

    @TestFactory
    Stream<DynamicTest> allModulesPassValidation()
    {
        List<Module> modules = ModuleUtils.list();

        return modules.stream().map(module ->
            DynamicTest.dynamicTest(module.getClass().getSimpleName(), () ->
            {
                List<String> errors = ModuleUtils.report(module);
                assertTrue(errors.isEmpty(),
                    module.getClass().getSimpleName() + " has validation errors:\n"
                    + String.join("\n", errors));
            })
        );
    }
}
