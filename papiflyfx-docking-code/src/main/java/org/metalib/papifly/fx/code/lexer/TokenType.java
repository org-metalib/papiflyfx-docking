package org.metalib.papifly.fx.code.lexer;

/**
 * Supported token categories for syntax rendering.
 */
public enum TokenType {
    PLAIN,
    KEYWORD,
    STRING,
    COMMENT,
    NUMBER,
    BOOLEAN,
    NULL_LITERAL,
    OPERATOR,
    PUNCTUATION,
    IDENTIFIER,
    
    // Markdown elements
    HEADLINE,
    LIST_ITEM,
    CODE_BLOCK,
    TEXT
}
