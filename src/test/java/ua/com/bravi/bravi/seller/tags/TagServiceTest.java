package ua.com.bravi.bravi.seller.tags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.seller.tags.domain.Tag;
import ua.com.bravi.bravi.seller.tags.domain.TagBulkMode;
import ua.com.bravi.bravi.seller.tags.domain.TagPalette;
import ua.com.bravi.bravi.seller.tags.domain.TagRef;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;
import ua.com.bravi.bravi.seller.tags.exception.TagAlreadyExistsException;
import ua.com.bravi.bravi.seller.tags.persistence.ITagEntityRepository;
import ua.com.bravi.bravi.seller.tags.persistence.TagLink;
import ua.com.bravi.bravi.seller.tags.persistence.TagLinkStore;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;
import ua.com.bravi.bravi.seller.tags.persistence.mapper.TagEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long PRODUCT_ID = 42L;
    private static final String HIT_PUBLIC_ID = "tag_hit";

    private final ITagEntityRepository tagRepository = mock(ITagEntityRepository.class);
    private final TagEntityMapper tagEntityMapper = mock(TagEntityMapper.class);
    private final TagLinkStore linkStore = mock(TagLinkStore.class);

    private TagService service;

    @BeforeEach
    void setUp() {
        lenient().when(linkStore.target()).thenReturn(TagTarget.PRODUCT);
        service = new TagService(tagRepository, tagEntityMapper, List.of(linkStore));
    }

    @Test
    void anUnknownNameIsInsertedAndThenReadBack() {
        storeHolds();
        when(tagRepository.findByStoreIdAndTargetAndNameKeyIn(eq(STORE_ID), eq(TagTarget.PRODUCT), any()))
                .thenReturn(List.of(entity(1L, HIT_PUBLIC_ID, "Новинка")));

        service.applyBulk(STORE_ID, TagTarget.PRODUCT, List.of(PRODUCT_ID),
                List.of(new TagRef(null, "Новинка")), TagBulkMode.ADD);

        var order = inOrder(tagRepository);
        order.verify(tagRepository).insertIfAbsent(anyString(), eq(STORE_ID), eq("PRODUCT"),
                eq("Новинка"), eq(TagPalette.pick("новинка")), eq(TagStatus.ACTIVE.name()),
                any(Instant.class));
        order.verify(tagRepository).findByStoreIdAndTargetAndNameKeyIn(eq(STORE_ID), eq(TagTarget.PRODUCT), any());
    }

    @Test
    void aNameTheStoreAlreadyOwnsIsNeverInserted() {
        storeHolds(tag(1L, HIT_PUBLIC_ID, "Хіт"));

        service.applyBulk(STORE_ID, TagTarget.PRODUCT, List.of(PRODUCT_ID),
                List.of(new TagRef(null, "  ХІТ ")), TagBulkMode.ADD);

        verify(tagRepository, never()).insertIfAbsent(anyString(), anyLong(), anyString(), anyString(),
                anyString(), anyString(), any(Instant.class));
        verify(linkStore).link(PRODUCT_ID, List.of(1L));
    }

    /** Removing a name that matches nothing has nothing to unpin, so it must not mint the tag. */
    @Test
    void removingByAnUnknownNameCreatesNothing() {
        storeHolds();

        int updated = service.applyBulk(STORE_ID, TagTarget.PRODUCT, List.of(PRODUCT_ID),
                List.of(new TagRef(null, "Невідомий")), TagBulkMode.REMOVE);

        verify(tagRepository, never()).insertIfAbsent(anyString(), anyLong(), anyString(), anyString(),
                anyString(), anyString(), any(Instant.class));
        assertThat(updated).isZero();
    }

    @Test
    void replacingWritesOnlyTheDifference() {
        storeHolds(tag(1L, HIT_PUBLIC_ID, "Хіт"), tag(2L, "tag_sale", "Розпродаж"));
        when(linkStore.findByOwnerIds(List.of(PRODUCT_ID)))
                .thenReturn(List.of(new TagLink(100L, PRODUCT_ID, 2L), new TagLink(101L, PRODUCT_ID, 9L)));
        when(tagRepository.findAllById(any())).thenReturn(List.of());

        service.replaceFor(STORE_ID, TagTarget.PRODUCT, PRODUCT_ID,
                List.of(new TagRef(HIT_PUBLIC_ID, null), new TagRef("tag_sale", null)));

        verify(linkStore).unlink(PRODUCT_ID, List.of(9L));
        verify(linkStore).link(PRODUCT_ID, List.of(1L));
    }

    @Test
    void mergeMovesTheAssignmentsBeforeDroppingTheSources() {
        TagEntity survivor = entity(1L, HIT_PUBLIC_ID, "Хіт");
        TagEntity source = entity(2L, "tag_sale", "Розпродаж");
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID))
                .thenReturn(Optional.of(survivor));
        when(tagRepository.findByStoreIdAndTargetAndPublicIdIn(eq(STORE_ID), eq(TagTarget.PRODUCT), any()))
                .thenReturn(List.of(source));
        when(linkStore.countByTagIds(anyCollection())).thenReturn(Map.of(1L, 3L));
        when(tagEntityMapper.toView(eq(survivor), anyLong())).thenReturn(mock(ua.com.bravi.bravi.seller.tags.api.TagView.class));

        service.merge(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID, List.of("tag_sale"));

        var order = inOrder(linkStore, tagRepository);
        order.verify(linkStore).repointToTag(List.of(2L), 1L);
        order.verify(linkStore).deleteByTagIds(List.of(2L));
        order.verify(tagRepository).deleteAll(List.of(source));
    }

    @Test
    void aTagCannotBeMergedIntoItself() {
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID))
                .thenReturn(Optional.of(entity(1L, HIT_PUBLIC_ID, "Хіт")));

        assertThatThrownBy(() -> service.merge(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID,
                List.of(HIT_PUBLIC_ID)))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("source_ids"));
        verify(linkStore, never()).repointToTag(any(), any());
    }

    @Test
    void aSubmittedColourWinsOverThePaletteAndIsStoredCanonically() {
        when(tagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(STORE_ID, TagTarget.PRODUCT, Tag.builder().name("Хіт").color("#e5484d").build());

        ArgumentCaptor<TagEntity> saved = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getColor()).isEqualTo("#E5484D");
    }

    @Test
    void aTagCreatedWithoutAColourTakesOneFromThePalette() {
        when(tagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(STORE_ID, TagTarget.PRODUCT, Tag.builder().name("Хіт").build());

        ArgumentCaptor<TagEntity> saved = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getColor()).isEqualTo(TagPalette.pick("хіт"));
    }

    @Test
    void patchingWithoutAColourLeavesItAlone() {
        TagEntity stored = entity(1L, HIT_PUBLIC_ID, "Хіт");
        stored.setColor("#0091FF");
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID))
                .thenReturn(Optional.of(stored));
        when(tagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID,
                Tag.builder().status(TagStatus.INACTIVE).build());

        assertThat(stored.getColor()).isEqualTo("#0091FF");
    }

    @Test
    void patchingWithAColourStoresItCanonically() {
        TagEntity stored = entity(1L, HIT_PUBLIC_ID, "Хіт");
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID))
                .thenReturn(Optional.of(stored));
        when(tagRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(STORE_ID, TagTarget.PRODUCT, HIT_PUBLIC_ID, Tag.builder().color("#f80").build());

        assertThat(stored.getColor()).isEqualTo("#FF8800");
    }

    @Test
    void aDuplicateNameSurfacesAsAConflictOnTheNameField() {
        when(tagRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup",
                new org.hibernate.exception.ConstraintViolationException("dup", new java.sql.SQLException(),
                        "uq_store_tags_store_target_name_lower")));

        assertThatThrownBy(() -> service.create(STORE_ID, TagTarget.PRODUCT,
                Tag.builder().name("Хіт").build()))
                .isInstanceOfSatisfying(TagAlreadyExistsException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("name"))
                .hasMessageContaining("merge");
    }

    @Test
    void anotherConstraintIsNotDisguisedAsADuplicateName() {
        when(tagRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("other"));

        assertThatThrownBy(() -> service.create(STORE_ID, TagTarget.PRODUCT,
                Tag.builder().name("Хіт").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** The target is the second barrier between the product and order vocabularies, after permissions. */
    @Test
    void aTagOfAnotherTargetIsNotFound() {
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.ORDER, HIT_PUBLIC_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPublicId(STORE_ID, TagTarget.ORDER, HIT_PUBLIC_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void aTargetWithoutLinksIsRejectedRatherThanFailingWithNull() {
        when(tagRepository.findByStoreIdAndTargetAndPublicId(STORE_ID, TagTarget.ORDER, HIT_PUBLIC_ID))
                .thenReturn(Optional.of(entity(1L, HIT_PUBLIC_ID, "Терміново")));

        assertThatThrownBy(() -> service.getByPublicId(STORE_ID, TagTarget.ORDER, HIT_PUBLIC_ID))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("target"));
    }

    @Test
    void anUnknownTagInAFilterIsNotFound() {
        when(tagRepository.findByStoreIdAndTargetAndPublicIdIn(eq(STORE_ID), eq(TagTarget.PRODUCT), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveFilter(STORE_ID, TagTarget.PRODUCT, List.of("tag_missing")))
                .isInstanceOf(NotFoundException.class);
    }

    private void storeHolds(Tag... tags) {
        when(tagRepository.findByStoreIdAndTargetOrderByNameAsc(STORE_ID, TagTarget.PRODUCT))
                .thenReturn(List.of());
        when(tagEntityMapper.toDomains(any())).thenReturn(List.of(tags));
    }

    private static Tag tag(Long id, String publicId, String name) {
        return Tag.builder().id(id).publicId(publicId).storeId(STORE_ID)
                .target(TagTarget.PRODUCT).name(name).status(TagStatus.ACTIVE).build();
    }

    private static TagEntity entity(Long id, String publicId, String name) {
        TagEntity entity = new TagEntity();
        entity.setId(id);
        entity.setPublicId(publicId);
        entity.setStoreId(STORE_ID);
        entity.setTarget(TagTarget.PRODUCT);
        entity.setName(name);
        entity.setColor("#E5484D");
        entity.setStatus(TagStatus.ACTIVE);
        return entity;
    }
}
