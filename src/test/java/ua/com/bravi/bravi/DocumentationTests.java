package ua.com.bravi.bravi;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;


public class DocumentationTests {

    ApplicationModules modules = ApplicationModules.of(BraviApplication.class);

    @Test
    void writeDocumentationSnippets() {

    new Documenter(modules)
      .writeModulesAsPlantUml()
      .writeIndividualModulesAsPlantUml();
    }
}
