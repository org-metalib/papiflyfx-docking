package org.metalib.papifly.fx.code.search;

import org.metalib.papifly.fx.code.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Search/replace model that finds matches in a document.
 * Supports plain text and regex modes, case-sensitive and case-insensitive.
 */
public class SearchModel {

    private String query = "";
    private String replacement = "";
    private boolean regexMode;
    private boolean caseSensitive;
    private List<SearchMatch> matches = List.of();
    private int currentMatchIndex = -1;

    /**
     * Returns the current search query.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the search query.
     */
    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    /**
     * Returns the replacement string.
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * Sets the replacement string.
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement == null ? "" : replacement;
    }

    /**
     * Returns true if regex mode is enabled.
     */
    public boolean isRegexMode() {
        return regexMode;
    }

    /**
     * Enables or disables regex mode.
     */
    public void setRegexMode(boolean regexMode) {
        this.regexMode = regexMode;
    }

    /**
     * Returns true if case-sensitive search is enabled.
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Enables or disables case-sensitive search.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Returns the current list of matches (unmodifiable).
     */
    public List<SearchMatch> getMatches() {
        return matches;
    }

    /**
     * Returns the index of the currently selected match, or -1 if none.
     */
    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }

    /**
     * Returns the currently selected match, or null if none.
     */
    public SearchMatch getCurrentMatch() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size()) {
            return matches.get(currentMatchIndex);
        }
        return null;
    }

    /**
     * Returns the total match count.
     */
    public int getMatchCount() {
        return matches.size();
    }

    /**
     * Executes the search against the given document and populates matches.
     * Returns the number of matches found.
     */
    public int search(Document document) {
        currentMatchIndex = -1;
        if (query.isEmpty() || document == null) {
            matches = List.of();
            return 0;
        }

        String text = document.getText();
        List<SearchMatch> found = new ArrayList<>();

        if (regexMode) {
            found = searchRegex(text, document);
        } else {
            found = searchPlainText(text, document);
        }

        matches = Collections.unmodifiableList(found);
        if (!matches.isEmpty()) {
            currentMatchIndex = 0;
        }
        return matches.size();
    }

    /**
     * Advances to the next match. Wraps around to the first match.
     * Returns the new current match, or null if no matches.
     */
    public SearchMatch nextMatch() {
        if (matches.isEmpty()) {
            return null;
        }
        currentMatchIndex = (currentMatchIndex + 1) % matches.size();
        return matches.get(currentMatchIndex);
    }

    /**
     * Goes to the previous match. Wraps around to the last match.
     * Returns the new current match, or null if no matches.
     */
    public SearchMatch previousMatch() {
        if (matches.isEmpty()) {
            return null;
        }
        currentMatchIndex = (currentMatchIndex - 1 + matches.size()) % matches.size();
        return matches.get(currentMatchIndex);
    }

    /**
     * Selects the match nearest to the given document offset.
     */
    public void selectNearestMatch(int offset) {
        if (matches.isEmpty()) {
            currentMatchIndex = -1;
            return;
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < matches.size(); i++) {
            int dist = Math.abs(matches.get(i).startOffset() - offset);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = i;
            }
        }
        currentMatchIndex = bestIndex;
    }

    /**
     * Replaces the current match in the document.
     * Returns true if replacement was performed.
     */
    public boolean replaceCurrent(Document document) {
        SearchMatch match = getCurrentMatch();
        if (match == null || document == null) {
            return false;
        }
        document.replace(match.startOffset(), match.endOffset(), replacement);
        return true;
    }

    /**
     * Replaces all matches in the document (from last to first to preserve offsets).
     * Returns the number of replacements made.
     */
    public int replaceAll(Document document) {
        if (matches.isEmpty() || document == null) {
            return 0;
        }
        int count = matches.size();
        // Replace from end to start to preserve offsets
        for (int i = matches.size() - 1; i >= 0; i--) {
            SearchMatch match = matches.get(i);
            document.replace(match.startOffset(), match.endOffset(), replacement);
        }
        matches = List.of();
        currentMatchIndex = -1;
        return count;
    }

    /**
     * Clears search state.
     */
    public void clear() {
        query = "";
        replacement = "";
        matches = List.of();
        currentMatchIndex = -1;
    }

    private List<SearchMatch> searchPlainText(String text, Document document) {
        List<SearchMatch> found = new ArrayList<>();
        String searchIn = caseSensitive ? text : text.toLowerCase();
        String searchFor = caseSensitive ? query : query.toLowerCase();

        int index = 0;
        while ((index = searchIn.indexOf(searchFor, index)) >= 0) {
            int endIndex = index + query.length();
            int line = document.getLineForOffset(index);
            int lineStart = document.getLineStartOffset(line);
            int startCol = index - lineStart;
            int endCol = endIndex - lineStart;
            found.add(new SearchMatch(index, endIndex, line, startCol, endCol));
            index++;
        }
        return found;
    }

    private List<SearchMatch> searchRegex(String text, Document document) {
        List<SearchMatch> found = new ArrayList<>();
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern pattern = Pattern.compile(query, flags);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (start == end) {
                    // Skip zero-length matches to avoid infinite loop
                    continue;
                }
                int line = document.getLineForOffset(start);
                int lineStart = document.getLineStartOffset(line);
                int startCol = start - lineStart;
                int endCol = end - lineStart;
                found.add(new SearchMatch(start, end, line, startCol, endCol));
            }
        } catch (PatternSyntaxException e) {
            // Invalid regex: return empty
        }
        return found;
    }
}
