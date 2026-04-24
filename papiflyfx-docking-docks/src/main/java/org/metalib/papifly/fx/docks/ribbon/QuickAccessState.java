package org.metalib.papifly.fx.docks.ribbon;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Package-private owner for Quick Access Toolbar id state and derived command view.
 */
final class QuickAccessState {

    private final ObservableList<String> commandIds = FXCollections.observableArrayList();
    private final ObservableList<RibbonCommand> commands = FXCollections.observableArrayList();
    private final ObservableList<RibbonCommand> commandsView = FXCollections.unmodifiableObservableList(commands);

    QuickAccessState(Runnable refreshView) {
        Objects.requireNonNull(refreshView, "refreshView");
        commandIds.addListener((ListChangeListener<String>) change -> refreshView.run());
    }

    ObservableList<String> commandIds() {
        return commandIds;
    }

    ObservableList<RibbonCommand> commandsView() {
        return commandsView;
    }

    void addReachableIdsTo(LinkedHashSet<String> reachable) {
        commandIds.stream()
            .filter(Objects::nonNull)
            .filter(id -> !id.isBlank())
            .forEach(reachable::add);
    }

    boolean pinIfAbsent(String commandId) {
        if (commandId == null || commandId.isBlank() || commandIds.contains(commandId)) {
            return false;
        }
        commandIds.add(commandId);
        return true;
    }

    List<RibbonCommand> resolveCommandsById(List<String> ids, Function<String, java.util.Optional<RibbonCommand>> resolver) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> emitted = new LinkedHashSet<>();
        List<RibbonCommand> resolved = new ArrayList<>();
        for (String commandId : ids) {
            if (commandId == null || commandId.isBlank()) {
                continue;
            }
            resolver.apply(commandId).ifPresent(command -> {
                if (emitted.add(command.id())) {
                    resolved.add(command);
                }
            });
        }
        return resolved;
    }

    void refreshCommandView(Function<String, java.util.Optional<RibbonCommand>> resolver) {
        commands.setAll(resolveCommandsById(commandIds, resolver));
    }
}
