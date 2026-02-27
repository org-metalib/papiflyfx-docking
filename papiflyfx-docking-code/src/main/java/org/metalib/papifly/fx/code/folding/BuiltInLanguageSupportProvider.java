package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.JavaLexer;
import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;
import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class BuiltInLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport(
                "plain-text", "Plain Text",
                Set.of("plain", "plaintext", "text", "txt"), Set.of("txt"),
                Set.of(),
                PlainTextLexer::new, PlainTextFoldProvider::new),
            new LanguageSupport(
                "java", "Java",
                Set.of(), Set.of("java"),
                Set.of(),
                JavaLexer::new, JavaFoldProvider::new),
            new LanguageSupport(
                "javascript", "JavaScript",
                Set.of("js"), Set.of("js", "mjs", "cjs"),
                Set.of(),
                JavaScriptLexer::new, JavaScriptFoldProvider::new),
            new LanguageSupport(
                "json", "JSON",
                Set.of(), Set.of("json"),
                Set.of(),
                JsonLexer::new, JsonFoldProvider::new),
            new LanguageSupport(
                "markdown", "Markdown",
                Set.of("md"), Set.of("md", "markdown"),
                Set.of(),
                MarkdownLexer::new, MarkdownFoldProvider::new)
        );
    }
}
