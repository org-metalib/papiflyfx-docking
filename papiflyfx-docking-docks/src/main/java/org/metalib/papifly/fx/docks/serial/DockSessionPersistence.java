package org.metalib.papifly.fx.docks.serial;

import org.metalib.papifly.fx.docks.layout.data.DockSessionData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Utility for persisting dock session data to/from JSON files and strings.
 * Wraps {@link DockSessionSerializer} to provide convenient file I/O and string serialization.
 */
public class DockSessionPersistence {

    private final DockSessionSerializer serializer;

    /**
     * Creates a new DockSessionPersistence with a default serializer.
     */
    public DockSessionPersistence() {
        this(new DockSessionSerializer());
    }

    /**
     * Creates a new DockSessionPersistence with a custom serializer.
     */
    public DockSessionPersistence(DockSessionSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Serializes a DockSessionData to a JSON string.
     *
     * @param session the session to serialize
     * @return JSON string representation, or null if session is null
     * @throws SessionSerializationException if serialization fails
     */
    public String toJsonString(DockSessionData session) {
        try {
            if (session == null) {
                return null;
            }
            Map<String, Object> map = serializer.serialize(session);
            return serializer.toJson(map);
        } catch (Exception e) {
            throw new SessionSerializationException("Failed to serialize session to JSON string", e);
        }
    }

    /**
     * Deserializes a DockSessionData from a JSON string.
     *
     * @param json the JSON string to deserialize
     * @return the deserialized DockSessionData, or null if json is null
     * @throws SessionSerializationException if deserialization fails
     */
    public DockSessionData fromJsonString(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            Map<String, Object> map = serializer.fromJson(json);
            return serializer.deserialize(map);
        } catch (Exception e) {
            throw new SessionSerializationException("Failed to deserialize session from JSON string", e);
        }
    }

    /**
     * Saves a session to a JSON file.
     *
     * @param session the session to save
     * @param path    the file path to write to
     * @throws SessionSerializationException if serialization fails
     * @throws SessionFileIOException        if file I/O fails
     */
    public void toJsonFile(DockSessionData session, Path path) {
        try {
            String json = toJsonString(session);
            if (json != null) {
                // Create parent directories if they don't exist
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                Files.write(path, json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SessionSerializationException e) {
            throw e;
        } catch (IOException e) {
            throw new SessionFileIOException("Failed to write session to file: " + path, e);
        } catch (Exception e) {
            throw new SessionFileIOException("Unexpected error writing session to file: " + path, e);
        }
    }

    /**
     * Loads a session from a JSON file.
     *
     * @param path the file path to read from
     * @return the deserialized DockSessionData
     * @throws SessionFileIOException        if file I/O fails
     * @throws SessionSerializationException if deserialization fails
     */
    public DockSessionData fromJsonFile(Path path) {
        try {
            if (!Files.exists(path)) {
                throw new SessionFileIOException("Session file not found: " + path);
            }
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return fromJsonString(json);
        } catch (SessionFileIOException e) {
            throw e;
        } catch (SessionSerializationException e) {
            throw e;
        } catch (IOException e) {
            throw new SessionFileIOException("Failed to read session from file: " + path, e);
        } catch (Exception e) {
            throw new SessionFileIOException("Unexpected error reading session from file: " + path, e);
        }
    }

    /**
     * Gets the underlying DockSessionSerializer.
     */
    public DockSessionSerializer getSerializer() {
        return serializer;
    }

    /**
     * Exception thrown when session serialization/deserialization fails.
     */
    public static class SessionSerializationException extends RuntimeException {
        public SessionSerializationException(String message) {
            super(message);
        }

        public SessionSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when session file I/O fails.
     */
    public static class SessionFileIOException extends RuntimeException {
        public SessionFileIOException(String message) {
            super(message);
        }

        public SessionFileIOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
