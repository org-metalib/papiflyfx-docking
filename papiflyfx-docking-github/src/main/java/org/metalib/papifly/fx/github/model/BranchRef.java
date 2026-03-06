package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record BranchRef(
    String name,
    String fullName,
    boolean local,
    boolean remote,
    boolean current
) implements Comparable<BranchRef> {

    public BranchRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fullName, "fullName");
    }

    @Override
    public int compareTo(BranchRef other) {
        int localOrder = Boolean.compare(other.local, local);
        if (localOrder != 0) {
            return localOrder;
        }
        int currentOrder = Boolean.compare(other.current, current);
        if (currentOrder != 0) {
            return currentOrder;
        }
        return name.compareToIgnoreCase(other.name);
    }
}
