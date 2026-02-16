package org.metalib.papifly.fx.code.lexer;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves language ids to lexer implementations.
 */
public final class LexerRegistry {

    private static final Lexer PLAIN_TEXT = new PlainTextLexer();
    private static final Lexer JAVA = new JavaLexer();
    private static final Lexer JSON = new JsonLexer();
    private static final Lexer JAVASCRIPT = new JavaScriptLexer();

    private static final Map<String, Lexer> LEXERS = Map.ofEntries(
        Map.entry(PlainTextLexer.LANGUAGE_ID, PLAIN_TEXT),
        Map.entry("plain", PLAIN_TEXT),
        Map.entry("plaintext", PLAIN_TEXT),
        Map.entry("text", PLAIN_TEXT),
        Map.entry("txt", PLAIN_TEXT),
        Map.entry(JavaLexer.LANGUAGE_ID, JAVA),
        Map.entry(JsonLexer.LANGUAGE_ID, JSON),
        Map.entry(JavaScriptLexer.LANGUAGE_ID, JAVASCRIPT),
        Map.entry("js", JAVASCRIPT)
    );

    private LexerRegistry() {
    }

    /**
     * Resolves a language id to a lexer. Unknown ids fall back to plain text.
     */
    public static Lexer resolve(String languageId) {
        return LEXERS.getOrDefault(normalizeLanguageId(languageId), PLAIN_TEXT);
    }

    /**
     * Normalizes language ids for registry lookup.
     */
    public static String normalizeLanguageId(String languageId) {
        if (languageId == null || languageId.isBlank()) {
            return PlainTextLexer.LANGUAGE_ID;
        }
        return languageId.trim().toLowerCase(Locale.ROOT);
    }
}
