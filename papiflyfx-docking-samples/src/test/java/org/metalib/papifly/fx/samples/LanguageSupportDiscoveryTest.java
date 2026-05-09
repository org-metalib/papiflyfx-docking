package org.metalib.papifly.fx.samples;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.JavaLexer;
import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;
import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LanguageSupportDiscoveryTest {

    @Test
    void samplesClasspathDiscoversAllShippedCodeEditorLanguages() {
        LanguageSupportRegistry registry = LanguageSupportRegistry.defaultRegistry();

        Map<String, Class<?>> expectedLexers = Map.of(
            "plain-text", PlainTextLexer.class,
            "java", JavaLexer.class,
            "javascript", JavaScriptLexer.class,
            "json", JsonLexer.class,
            "markdown", MarkdownLexer.class,
            "yaml", YamlLexer.class
        );

        expectedLexers.forEach((id, expectedClass) -> {
            assertInstanceOf(expectedClass, registry.resolveLexer(id));
            assertEquals(id, registry.resolveFoldProvider(id).languageId());
        });

        assertEquals("json", registry.detectLanguageId("package.json").orElse(""));
        assertEquals("markdown", registry.detectLanguageId("README.md").orElse(""));
        assertEquals("yaml", registry.detectLanguageId("deployment.yaml").orElse(""));
        assertEquals("yaml", registry.detectLanguageId("deployment.yml").orElse(""));
    }
}
