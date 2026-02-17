package org.metalib.papifly.fx.code.gutter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Model holding line markers (errors, breakpoints, bookmarks, etc.).
 * Thread-safe for concurrent add/remove from background threads.
 */
public class MarkerModel {

    /**
     * Listener notified when markers change.
     */
    @FunctionalInterface
    public interface MarkerChangeListener {
        void markersChanged();
    }

    private final Map<Integer, List<Marker>> markersByLine = new ConcurrentHashMap<>();
    private final List<MarkerChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a marker change listener.
     */
    public void addChangeListener(MarkerChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a marker change listener.
     */
    public void removeChangeListener(MarkerChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Adds a marker.
     */
    public void addMarker(Marker marker) {
        markersByLine.computeIfAbsent(marker.line(), k -> new CopyOnWriteArrayList<>()).add(marker);
        fireChanged();
    }

    /**
     * Removes a specific marker.
     */
    public void removeMarker(Marker marker) {
        List<Marker> lineMarkers = markersByLine.get(marker.line());
        if (lineMarkers != null && lineMarkers.remove(marker)) {
            if (lineMarkers.isEmpty()) {
                markersByLine.remove(marker.line());
            }
            fireChanged();
        }
    }

    /**
     * Removes all markers for a line.
     */
    public void clearLine(int line) {
        if (markersByLine.remove(line) != null) {
            fireChanged();
        }
    }

    /**
     * Removes all markers.
     */
    public void clearAll() {
        if (!markersByLine.isEmpty()) {
            markersByLine.clear();
            fireChanged();
        }
    }

    /**
     * Returns markers for a given line (unmodifiable).
     */
    public List<Marker> getMarkersForLine(int line) {
        List<Marker> markers = markersByLine.get(line);
        return markers == null ? List.of() : Collections.unmodifiableList(markers);
    }

    /**
     * Returns true if any marker exists for the given line.
     */
    public boolean hasMarkers(int line) {
        List<Marker> markers = markersByLine.get(line);
        return markers != null && !markers.isEmpty();
    }

    /**
     * Returns all markers across all lines.
     */
    public List<Marker> getAllMarkers() {
        List<Marker> all = new ArrayList<>();
        for (List<Marker> lineMarkers : markersByLine.values()) {
            all.addAll(lineMarkers);
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Returns the highest-priority marker type for a line, or null if none.
     */
    public MarkerType getHighestPriorityType(int line) {
        List<Marker> markers = markersByLine.get(line);
        if (markers == null || markers.isEmpty()) {
            return null;
        }
        MarkerType best = null;
        for (Marker m : markers) {
            if (best == null || m.type().priority() < best.priority()) {
                best = m.type();
            }
        }
        return best;
    }

    private void fireChanged() {
        for (MarkerChangeListener listener : listeners) {
            listener.markersChanged();
        }
    }
}
