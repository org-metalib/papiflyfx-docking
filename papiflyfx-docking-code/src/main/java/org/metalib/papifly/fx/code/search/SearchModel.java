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
 * Supports plain text and regex modes, case-sensitive and case-insensitive,
 * and whole-word matching.
 */
public class SearchModel {

    private String query = "";
    private String replacement = "";
    private boolean regexMode;
    private boolean caseSensitive;
    private boolean wholeWord;
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
     * Returns true if whole-word matching is enabled.
     */
    public boolean isWholeWord() {
        return wholeWord;
    }

    /**
     * Enables or disables whole-word matching.
     */
    public void setWholeWord(boolean wholeWord) {
        this.wholeWord = wholeWord;
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
        List<SearchMatch> found;

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
     * When regex mode is active, capture-group references ($1, $2, etc.) in the
     * replacement string are expanded.
     * Returns true if replacement was performed.
     */
    public boolean replaceCurrent(Document document) {
        SearchMatch match = getCurrentMatch();
        if (match == null || document == null) {
            return false;
        }
        String effectiveReplacement = computeReplacement(document.getText(), match);
        document.replace(match.startOffset(), match.endOffset(), effectiveReplacement);
        search(document);
        return true;
    }

    /**
     * Replaces all matches in the document.
     * When regex mode is active, uses {@link Matcher#replaceAll} for correct
     * capture-group expansion in a single pass.
     * Returns the number of replacements made.
     */
    public int replaceAll(Document document) {
        if (matches.isEmpty() || document == null) {
            return 0;
        }
        if (regexMode) {
            return replaceAllRegex(document);
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

    private String computeReplacement(String text, SearchMatch match) {
        if (!regexMode) {
            return replacement;
        }
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            String patternStr = wholeWord ? "\\b" + query + "\\b" : query;
            Pattern pattern = Pattern.compile(patternStr, flags);
            String matchedText = text.substring(match.startOffset(), match.endOffset());
            Matcher matcher = pattern.matcher(matchedText);
            if (matcher.matches()) {
                return matcher.replaceFirst(replacement);
            }
        } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
            // Fall through to literal replacement
        }
        return replacement;
    }

    private int replaceAllRegex(Document document) {
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            String patternStr = wholeWord ? "\\b" + query + "\\b" : query;
            Pattern pattern = Pattern.compile(patternStr, flags);
            String text = document.getText();
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            // Count matches first (we already know from matches.size() but re-count for safety)
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                if (matcher.start() == matcher.end()) {
                    continue; // skip zero-length
                }
                int startLine = document.getLineForOffset(matcher.start());
                int endLine = document.getLineForOffset(Math.max(matcher.start(), matcher.end() - 1));
                if (startLine != endLine) {
                    continue; // skip multi-line
                }
                matcher.appendReplacement(sb, replacement);
                count++;
            }
            matcher.appendTail(sb);
            if (count > 0) {
                document.replace(0, text.length(), sb.toString());
            }
            matches = List.of();
            currentMatchIndex = -1;
            return count;
        } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
            return 0;
        }
    }

    private List<SearchMatch> searchPlainText(String text, Document document) {
        List<SearchMatch> found = new ArrayList<>();
        String searchIn = caseSensitive ? text : text.toLowerCase();
        String searchFor = caseSensitive ? query : query.toLowerCase();

        int index = 0;
        while ((index = searchIn.indexOf(searchFor, index)) >= 0) {
            int endIndex = index + query.length();
            if (wholeWord && !isWordBoundary(text, index, endIndex)) {
                index += searchFor.length();
                continue;
            }
            int line = document.getLineForOffset(index);
            int endLine = document.getLineForOffset(Math.max(index, endIndex - 1));
            if (line != endLine) {
                index += searchFor.length();
                continue;
            }
            int lineStart = document.getLineStartOffset(line);
            int startCol = index - lineStart;
            int endCol = endIndex - lineStart;
            found.add(new SearchMatch(index, endIndex, line, startCol, endCol));
            index += searchFor.length();
        }
        return found;
    }

    private List<SearchMatch> searchRegex(String text, Document document) {
        List<SearchMatch> found = new ArrayList<>();
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            String patternStr = wholeWord ? "\\b" + query + "\\b" : query;
            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (start == end) {
                    // Skip zero-length matches to avoid infinite loop
                    continue;
                }
                int startLine = document.getLineForOffset(start);
                int endLine = document.getLineForOffset(Math.max(start, end - 1));
                if (startLine != endLine) {
                    continue;
                }
                int lineStart = document.getLineStartOffset(startLine);
                int startCol = start - lineStart;
                int endCol = end - lineStart;
                found.add(new SearchMatch(start, end, startLine, startCol, endCol));
            }
        } catch (PatternSyntaxException e) {
            // Invalid regex: return empty
        }
        return found;
    }

    private static boolean isWordBoundary(String text, int start, int end) {
        if (start > 0 && isWordChar(text.charAt(start - 1))) {
            return false;
        }
        if (end < text.length() && isWordChar(text.charAt(end))) {
            return false;
        }
        return true;
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
