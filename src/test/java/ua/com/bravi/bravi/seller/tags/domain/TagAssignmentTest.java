package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagAssignmentTest {

    private static final List<Long> CURRENT = List.of(1L, 2L);

    @Test
    void addOnlyPinsWhatIsMissing() {
        TagAssignment plan = TagAssignment.plan(CURRENT, List.of(2L, 3L), TagBulkMode.ADD);

        assertThat(plan.added()).containsExactly(3L);
        assertThat(plan.removed()).isEmpty();
    }

    @Test
    void removeOnlyUnpinsWhatIsThere() {
        TagAssignment plan = TagAssignment.plan(CURRENT, List.of(2L, 9L), TagBulkMode.REMOVE);

        assertThat(plan.added()).isEmpty();
        assertThat(plan.removed()).containsExactly(2L);
    }

    @Test
    void replaceLeavesExactlyTheSubmittedSet() {
        TagAssignment plan = TagAssignment.plan(CURRENT, List.of(2L, 3L), TagBulkMode.REPLACE);

        assertThat(plan.added()).containsExactly(3L);
        assertThat(plan.removed()).containsExactly(1L);
    }

    @Test
    void replaceWithNothingClearsTheSet() {
        TagAssignment plan = TagAssignment.plan(CURRENT, List.of(), TagBulkMode.REPLACE);

        assertThat(plan.added()).isEmpty();
        assertThat(plan.removed()).containsExactly(1L, 2L);
    }

    @Test
    void anAssignmentThatChangesNothingIsEmpty() {
        assertThat(TagAssignment.plan(CURRENT, List.of(1L, 2L), TagBulkMode.REPLACE).isEmpty()).isTrue();
        assertThat(TagAssignment.plan(CURRENT, List.of(1L), TagBulkMode.ADD).isEmpty()).isTrue();
        assertThat(TagAssignment.plan(CURRENT, List.of(9L), TagBulkMode.REMOVE).isEmpty()).isTrue();
    }

    @Test
    void duplicatesInTheSubmissionArePlannedOnce() {
        TagAssignment plan = TagAssignment.plan(List.of(), List.of(3L, 3L), TagBulkMode.ADD);

        assertThat(plan.added()).containsExactly(3L);
    }
}
