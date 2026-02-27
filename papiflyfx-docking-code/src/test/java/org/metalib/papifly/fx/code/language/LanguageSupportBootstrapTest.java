package org.metalib.papifly.fx.code.language;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LanguageSupportBootstrapTest {

    @Test
    void defaultBootstrapIncludesBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("java", registry.resolveLexer("java").languageId());
        assertEquals("javascript", registry.resolveLexer("javascript").languageId());
        assertEquals("json", registry.resolveLexer("json").languageId());
        assertEquals("markdown", registry.resolveLexer("markdown").languageId());
        assertEquals("plain-text", registry.resolveLexer("plain-text").languageId());
    }

    @Test
    void builtInsOmittedWhenDisabled() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(false, false, ConflictPolicy.REPLACE_EXISTING));

        Lexer lexer = registry.resolveLexer("java");
        assertEquals("plain-text", lexer.languageId());
    }

    @Test
    void aliasResolutionWithBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("javascript", registry.resolveLexer("js").languageId());
        assertEquals("markdown", registry.resolveLexer("md").languageId());
        assertEquals("plain-text", registry.resolveLexer("txt").languageId());
        assertEquals("plain-text", registry.resolveLexer("plain").languageId());
        assertEquals("plain-text", registry.resolveLexer("plaintext").languageId());
        assertEquals("plain-text", registry.resolveLexer("text").languageId());
    }

    @Test
    void extensionDetectionWithBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("java", registry.detectLanguageId("Main.java").orElse(""));
        assertEquals("javascript", registry.detectLanguageId("app.js").orElse(""));
        assertEquals("javascript", registry.detectLanguageId("module.mjs").orElse(""));
        assertEquals("javascript", registry.detectLanguageId("module.cjs").orElse(""));
        assertEquals("json", registry.detectLanguageId("config.json").orElse(""));
        assertEquals("markdown", registry.detectLanguageId("README.md").orElse(""));
        assertEquals("markdown", registry.detectLanguageId("docs.markdown").orElse(""));
        assertEquals("plain-text", registry.detectLanguageId("notes.txt").orElse(""));
    }

    @Test
    void conflictReplacementWorks() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        LanguageSupport custom = new LanguageSupport(
            "java", "Custom Java",
            java.util.Set.of(), java.util.Set.of("java"),
            java.util.Set.of(),
            org.metalib.papifly.fx.code.lexer.PlainTextLexer::new,
            () -> new org.metalib.papifly.fx.code.folding.FoldProvider() {
                @Override public String languageId() { return "custom-java"; }
                @Override public org.metalib.papifly.fx.code.folding.FoldMap recompute(
                    java.util.List<String> lines,
                    org.metalib.papifly.fx.code.lexer.TokenMap tokenMap,
                    org.metalib.papifly.fx.code.folding.FoldMap baseline,
                    int dirtyStartLine,
                    java.util.function.BooleanSupplier cancelled
                ) { return org.metalib.papifly.fx.code.folding.FoldMap.empty(); }
            }
        );
        registry.register(custom, ConflictPolicy.REPLACE_EXISTING);
        assertEquals("plain-text", registry.resolveLexer("java").languageId());
    }

    @Test
    void defaultRegistryIsPreBootstrapped() {
        LanguageSupportRegistry defaultRegistry = LanguageSupportRegistry.defaultRegistry();
        assertEquals("java", defaultRegistry.resolveLexer("java").languageId());
        assertEquals("javascript", defaultRegistry.resolveLexer("js").languageId());
    }
}
