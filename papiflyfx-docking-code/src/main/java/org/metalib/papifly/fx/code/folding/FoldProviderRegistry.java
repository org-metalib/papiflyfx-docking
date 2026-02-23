package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.JavaLexer;
import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;
import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;

import java.util.Locale;
import java.util.Map;

public final class FoldProviderRegistry {

    private static final FoldProvider PLAIN = new PlainTextFoldProvider();
    private static final FoldProvider JAVA = new JavaFoldProvider();
    private static final FoldProvider JS = new JavaScriptFoldProvider();
    private static final FoldProvider JSON = new JsonFoldProvider();
    private static final FoldProvider MARKDOWN = new MarkdownFoldProvider();

    private static final Map<String, FoldProvider> PROVIDERS = Map.ofEntries(
        Map.entry(PlainTextLexer.LANGUAGE_ID, PLAIN),
        Map.entry("plain", PLAIN),
        Map.entry("plaintext", PLAIN),
        Map.entry("text", PLAIN),
        Map.entry("txt", PLAIN),
        Map.entry(JavaLexer.LANGUAGE_ID, JAVA),
        Map.entry(JavaScriptLexer.LANGUAGE_ID, JS),
        Map.entry("js", JS),
        Map.entry(JsonLexer.LANGUAGE_ID, JSON),
        Map.entry(MarkdownLexer.LANGUAGE_ID, MARKDOWN),
        Map.entry("md", MARKDOWN)
    );

    private FoldProviderRegistry() {
    }

    public static FoldProvider resolve(String languageId) {
        return PROVIDERS.getOrDefault(normalizeLanguageId(languageId), PLAIN);
    }

    public static String normalizeLanguageId(String languageId) {
        if (languageId == null || languageId.isBlank()) {
            return PlainTextLexer.LANGUAGE_ID;
        }
        return languageId.trim().toLowerCase(Locale.ROOT);
    }
}

