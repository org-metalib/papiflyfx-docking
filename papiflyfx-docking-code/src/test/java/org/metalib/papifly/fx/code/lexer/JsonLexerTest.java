package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLexerTest {

    @Test
    void jsonLineIncludesStringBooleanNullNumberAndPunctuationTokens() {
        JsonLexer lexer = new JsonLexer();
        String line = "{\"ok\": true, \"value\": 12.5, \"none\": null}";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.STRING));
        assertTrue(types.contains(TokenType.BOOLEAN));
        assertTrue(types.contains(TokenType.NULL_LITERAL));
        assertTrue(types.contains(TokenType.NUMBER));
        assertTrue(types.contains(TokenType.PUNCTUATION));
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void jsonStringStatePropagatesAcrossLines() {
        JsonLexer lexer = new JsonLexer();

        LexResult first = lexer.lexLine("\"open string", LexState.DEFAULT);
        assertEquals(LexState.of(1), first.exitState());

        LexResult second = lexer.lexLine("close\"", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.STRING));
    }
}
