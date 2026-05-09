package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class YamlLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            YamlLexer.LANGUAGE_ID, "YAML",
            Set.of("yml"), Set.of("yaml", "yml"),
            Set.of(),
            YamlLexer::new, YamlFoldProvider::new
        ));
    }
}
