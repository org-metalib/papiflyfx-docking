package org.metalib.papifly.fx.settings.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes files atomically using a temp-file-then-rename strategy.
 *
 * <p>Before overwriting the target, the existing file (if any) is copied to a
 * {@code .bak} companion for corruption recovery. The new content is written to a
 * {@code .tmp} sibling and then atomically moved over the target.
 */
public final class AtomicFileWriter {

    private static final Logger LOG = Logger.getLogger(AtomicFileWriter.class.getName());

    private AtomicFileWriter() {}

    /**
     * Writes {@code content} to {@code target} atomically.
     *
     * <ol>
     *   <li>Creates parent directories if needed.</li>
     *   <li>If {@code target} already exists, copies it to {@code target.bak}.</li>
     *   <li>Writes {@code content} to {@code target.tmp}.</li>
     *   <li>Atomically renames {@code target.tmp} to {@code target}.</li>
     * </ol>
     *
     * @throws IOException if the write or rename fails
     */
    public static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path bakFile = target.resolveSibling(target.getFileName() + ".bak");
        Path tmpFile = target.resolveSibling(target.getFileName() + ".tmp");

        // Back up existing file before overwriting
        if (Files.exists(target)) {
            try {
                Files.copy(target, bakFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to create backup of " + target, e);
                // Continue — atomic write is still safer than direct overwrite
            }
        }

        // Write to temp file
        Files.writeString(tmpFile, content, StandardCharsets.UTF_8);

        // Atomic rename
        Files.move(tmpFile, target,
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
