package ua.com.bravi.bravi;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithStructureTest {

    private final ApplicationModules modules = ApplicationModules.of(BraviApplication.class);

    @Test
    void verifiesModulithStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
