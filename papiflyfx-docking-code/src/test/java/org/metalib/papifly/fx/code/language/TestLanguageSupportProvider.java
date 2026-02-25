package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;
import org.metalib.papifly.fx.code.lexer.LexResult;
import org.metalib.papifly.fx.code.lexer.LexState;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class TestLanguageSupportProvider implements LanguageSupportProvider {

    public static final String TEST_LANGUAGE_ID = "test-plugin-lang";

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport(
                TEST_LANGUAGE_ID, "Test Plugin Language",
                Set.of("tpl"), Set.of("tpl"),
                Set.of(),
                TestLexer::new, TestFoldProvider::new
            )
        );
    }

    static final class TestLexer implements Lexer {
        @Override
        public String languageId() {
            return TEST_LANGUAGE_ID;
        }

        @Override
        public LexResult lexLine(String lineText, LexState entryState) {
            return new LexResult(List.of(), LexState.DEFAULT);
        }
    }

    static final class TestFoldProvider implements FoldProvider {
        @Override
        public String languageId() {
            return TEST_LANGUAGE_ID;
        }

        @Override
        public FoldMap recompute(List<String> lines, TokenMap tokenMap, FoldMap baseline,
                                 int dirtyStartLine, BooleanSupplier cancelled) {
            return FoldMap.empty();
        }
    }
}
