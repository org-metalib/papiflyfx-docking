package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlLanguageSupportProviderTest {

    @Test
    void providerContributesYamlSupport() {
        LanguageSupport support = new YamlLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("yaml", support.id());
        assertEquals("YAML", support.displayName());
        assertEquals(Set.of("yml"), support.aliases());
        assertEquals(Set.of("yaml", "yml"), support.fileExtensions());
        assertInstanceOf(YamlLexer.class, support.lexerFactory().get());
        assertInstanceOf(YamlFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversYamlSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("yaml"));
        assertInstanceOf(YamlLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("yml"));
        assertEquals("yaml", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("yaml").languageId());
    }
}
