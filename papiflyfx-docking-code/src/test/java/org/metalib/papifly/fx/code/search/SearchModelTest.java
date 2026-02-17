package org.metalib.papifly.fx.code.search;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchModelTest {

    @Test
    void searchPlainTextFindsMatches() {
        Document doc = new Document("hello world hello");
        SearchModel model = new SearchModel();
        model.setQuery("hello");
        int count = model.search(doc);

        assertEquals(2, count);
        assertEquals(2, model.getMatchCount());
        assertEquals(0, model.getCurrentMatchIndex());
    }

    @Test
    void searchPlainTextCaseInsensitive() {
        Document doc = new Document("Hello HELLO hello");
        SearchModel model = new SearchModel();
        model.setQuery("hello");
        model.setCaseSensitive(false);
        int count = model.search(doc);

        assertEquals(3, count);
    }

    @Test
    void searchPlainTextCaseSensitive() {
        Document doc = new Document("Hello HELLO hello");
        SearchModel model = new SearchModel();
        model.setQuery("hello");
        model.setCaseSensitive(true);
        int count = model.search(doc);

        assertEquals(1, count);
        SearchMatch match = model.getCurrentMatch();
        assertNotNull(match);
        assertEquals(12, match.startOffset());
    }

    @Test
    void searchRegexMode() {
        Document doc = new Document("abc 123 def 456");
        SearchModel model = new SearchModel();
        model.setQuery("\\d+");
        model.setRegexMode(true);
        int count = model.search(doc);

        assertEquals(2, count);
        assertEquals(4, model.getMatches().get(0).startOffset());
        assertEquals(12, model.getMatches().get(1).startOffset());
    }

    @Test
    void searchRegexCaseInsensitive() {
        Document doc = new Document("Foo foo FOO");
        SearchModel model = new SearchModel();
        model.setQuery("foo");
        model.setRegexMode(true);
        model.setCaseSensitive(false);
        int count = model.search(doc);

        assertEquals(3, count);
    }

    @Test
    void searchRegexInvalidPatternReturnsEmpty() {
        Document doc = new Document("test");
        SearchModel model = new SearchModel();
        model.setQuery("[invalid");
        model.setRegexMode(true);
        int count = model.search(doc);

        assertEquals(0, count);
    }

    @Test
    void nextAndPreviousMatchWrapsAround() {
        Document doc = new Document("aaa");
        SearchModel model = new SearchModel();
        model.setQuery("a");
        model.search(doc);

        assertEquals(0, model.getCurrentMatchIndex());

        model.nextMatch();
        assertEquals(1, model.getCurrentMatchIndex());

        model.nextMatch();
        assertEquals(2, model.getCurrentMatchIndex());

        // Wraps to start
        model.nextMatch();
        assertEquals(0, model.getCurrentMatchIndex());

        // Wraps to end
        model.previousMatch();
        assertEquals(2, model.getCurrentMatchIndex());
    }

    @Test
    void plainTextSearchDoesNotReturnOverlappingMatches() {
        Document doc = new Document("aaa");
        SearchModel model = new SearchModel();
        model.setQuery("aa");

        int count = model.search(doc);

        assertEquals(1, count);
        assertEquals(0, model.getMatches().get(0).startOffset());
    }

    @Test
    void emptyQueryReturnsNoMatches() {
        Document doc = new Document("test");
        SearchModel model = new SearchModel();
        model.setQuery("");
        int count = model.search(doc);

        assertEquals(0, count);
        assertEquals(-1, model.getCurrentMatchIndex());
        assertNull(model.getCurrentMatch());
    }

    @Test
    void searchMatchLineAndColumns() {
        Document doc = new Document("first line\nsecond line\nthird line");
        SearchModel model = new SearchModel();
        model.setQuery("line");
        model.search(doc);

        assertEquals(3, model.getMatchCount());

        SearchMatch m0 = model.getMatches().get(0);
        assertEquals(0, m0.line());
        assertEquals(6, m0.startColumn());
        assertEquals(10, m0.endColumn());

        SearchMatch m1 = model.getMatches().get(1);
        assertEquals(1, m1.line());
        assertEquals(7, m1.startColumn());

        SearchMatch m2 = model.getMatches().get(2);
        assertEquals(2, m2.line());
    }

    @Test
    void selectNearestMatch() {
        Document doc = new Document("aXXa");
        SearchModel model = new SearchModel();
        model.setQuery("a");
        model.search(doc);

        model.selectNearestMatch(3);
        assertEquals(1, model.getCurrentMatchIndex());
        assertEquals(3, model.getCurrentMatch().startOffset());
    }

    @Test
    void replaceCurrent() {
        Document doc = new Document("hello world");
        SearchModel model = new SearchModel();
        model.setQuery("world");
        model.setReplacement("earth");
        model.search(doc);

        assertTrue(model.replaceCurrent(doc));
        assertEquals("hello earth", doc.getText());
    }

    @Test
    void replaceCurrentRefreshesMatches() {
        Document doc = new Document("aXaXa");
        SearchModel model = new SearchModel();
        model.setQuery("a");
        model.setReplacement("bb");
        model.search(doc);

        assertTrue(model.replaceCurrent(doc));
        assertEquals("bbXaXa", doc.getText());
        assertEquals(2, model.getMatchCount());
        assertNotNull(model.getCurrentMatch());
        assertEquals(3, model.getCurrentMatch().startOffset());
    }

    @Test
    void replaceAll() {
        Document doc = new Document("aXaXa");
        SearchModel model = new SearchModel();
        model.setQuery("a");
        model.setReplacement("b");
        model.search(doc);

        int replaced = model.replaceAll(doc);
        assertEquals(3, replaced);
        assertEquals("bXbXb", doc.getText());
        assertEquals(0, model.getMatchCount());
    }

    @Test
    void replaceCurrentWithNoMatchDoesNothing() {
        Document doc = new Document("test");
        SearchModel model = new SearchModel();
        model.setQuery("xyz");
        model.search(doc);

        assertFalse(model.replaceCurrent(doc));
        assertEquals("test", doc.getText());
    }

    @Test
    void clearResetsState() {
        Document doc = new Document("test");
        SearchModel model = new SearchModel();
        model.setQuery("test");
        model.setReplacement("replace");
        model.search(doc);

        model.clear();
        assertEquals("", model.getQuery());
        assertEquals("", model.getReplacement());
        assertEquals(0, model.getMatchCount());
        assertEquals(-1, model.getCurrentMatchIndex());
    }

    @Test
    void nextMatchOnEmptyReturnsNull() {
        SearchModel model = new SearchModel();
        assertNull(model.nextMatch());
        assertNull(model.previousMatch());
    }

    @Test
    void searchMatchLength() {
        SearchMatch match = new SearchMatch(5, 10, 0, 5, 10);
        assertEquals(5, match.length());
    }

    @Test
    void regexSearchSkipsMultiLineMatches() {
        Document doc = new Document("alpha\nbeta");
        SearchModel model = new SearchModel();
        model.setRegexMode(true);
        model.setQuery("alpha\\s+beta");

        int count = model.search(doc);

        assertEquals(0, count);
    }
}
