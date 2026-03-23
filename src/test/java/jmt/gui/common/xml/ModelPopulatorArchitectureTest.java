package jmt.gui.common.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class ModelPopulatorArchitectureTest {

    @Test
    void modelPopulator_has_no_legacy_dom_fallback_api_or_references() throws IOException {
        for (Method method : ModelPopulator.class.getDeclaredMethods()) {
            assertThat(method.getName()).isNotEqualTo("setSourceRoot");
        }

        String source = new String(
                Files.readAllBytes(Paths.get("src/main/java/jmt/gui/common/xml/ModelPopulator.java")),
                StandardCharsets.UTF_8
        );
        assertThat(source).doesNotContain("OLDXMLReader");
        assertThat(source).doesNotContain("SOURCE_ROOT");
        assertThat(source).doesNotContain("org.w3c.dom");
    }
}


