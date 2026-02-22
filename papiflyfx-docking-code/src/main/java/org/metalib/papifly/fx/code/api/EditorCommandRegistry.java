package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.EditorCommand;

import java.util.Map;
import java.util.Objects;

/**
 * Registers command handlers on {@link EditorCommandExecutor}.
 */
final class EditorCommandRegistry {

    void register(EditorCommandExecutor executor, Map<EditorCommand, Runnable> handlers) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(handlers, "handlers");
        for (Map.Entry<EditorCommand, Runnable> entry : handlers.entrySet()) {
            executor.register(entry.getKey(), entry.getValue());
        }
    }
}

