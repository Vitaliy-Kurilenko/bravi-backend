package ua.com.bravi.bravi.seller.tags.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The difference between the tags an owner carries and the tags a request asks for. Working out
 * the difference rather than rewriting the whole set keeps the untouched links, and with them the
 * moment each tag was pinned on.
 */
public record TagAssignment(List<Long> added, List<Long> removed) {

    public static TagAssignment plan(Collection<Long> current, Collection<Long> submitted, TagBulkMode mode) {
        Set<Long> currentIds = new LinkedHashSet<>(current);
        Set<Long> submittedIds = new LinkedHashSet<>(submitted);

        List<Long> added = new ArrayList<>();
        List<Long> removed = new ArrayList<>();

        switch (mode) {
            case ADD -> submittedIds.stream().filter(id -> !currentIds.contains(id)).forEach(added::add);
            case REMOVE -> submittedIds.stream().filter(currentIds::contains).forEach(removed::add);
            case REPLACE -> {
                submittedIds.stream().filter(id -> !currentIds.contains(id)).forEach(added::add);
                currentIds.stream().filter(id -> !submittedIds.contains(id)).forEach(removed::add);
            }
        }
        return new TagAssignment(List.copyOf(added), List.copyOf(removed));
    }

    /** Whether applying this plan would leave the owner's set untouched. */
    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }
}
