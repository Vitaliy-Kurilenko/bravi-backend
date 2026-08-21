package ua.com.bravi.bravi.seller.tags;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.tags.api.TagPage;
import ua.com.bravi.bravi.seller.tags.api.TagView;
import ua.com.bravi.bravi.seller.tags.api.TagsApi;
import ua.com.bravi.bravi.seller.tags.domain.Tag;
import ua.com.bravi.bravi.seller.tags.domain.TagAssignment;
import ua.com.bravi.bravi.seller.tags.domain.TagBulkMode;
import ua.com.bravi.bravi.seller.tags.domain.TagColor;
import ua.com.bravi.bravi.seller.tags.domain.TagName;
import ua.com.bravi.bravi.seller.tags.domain.TagPalette;
import ua.com.bravi.bravi.seller.tags.domain.TagRef;
import ua.com.bravi.bravi.seller.tags.domain.TagResolution;
import ua.com.bravi.bravi.seller.tags.domain.TagSearchQuery;
import ua.com.bravi.bravi.seller.tags.domain.TagSortBy;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;
import ua.com.bravi.bravi.seller.tags.exception.TagAlreadyExistsException;
import ua.com.bravi.bravi.seller.tags.persistence.ITagEntityRepository;
import ua.com.bravi.bravi.seller.tags.persistence.TagLink;
import ua.com.bravi.bravi.seller.tags.persistence.TagLinkStore;
import ua.com.bravi.bravi.seller.tags.persistence.TagSpecifications;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;
import ua.com.bravi.bravi.seller.tags.persistence.mapper.TagEntityMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.util.ConstraintViolations;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns the store's tag dictionary and the assignments that pin tags on things. One dictionary
 * serves every taggable aggregate, told apart by target; only the link table differs, which is why
 * the assignments go through {@link TagLinkStore} rather than a repository of their own.
 *
 * <p>Products and orders reach this service through {@code TagsApi} and hand over an owner id they
 * have already proven belongs to the store; nothing here reads their tables.
 */
@Slf4j
@Service
public class TagService implements TagsApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_SOURCE_IDS = "source_ids";
    private static final String UNIQUE_NAME_INDEX = "uq_store_tags_store_target_name_lower";

    private final ITagEntityRepository tagRepository;
    private final TagEntityMapper tagEntityMapper;
    private final Map<TagTarget, TagLinkStore> linkStores;

    public TagService(ITagEntityRepository tagRepository, TagEntityMapper tagEntityMapper,
                      List<TagLinkStore> linkStores) {
        this.tagRepository = tagRepository;
        this.tagEntityMapper = tagEntityMapper;
        this.linkStores = linkStores.stream()
                .collect(Collectors.toMap(TagLinkStore::target, Function.identity()));
    }

    @Override
    public TagPage search(Long storeId, TagTarget target, TagSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        TagSortBy sortBy = query.sortBy() != null ? query.sortBy() : TagSortBy.NAME;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.ASC;

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<TagEntity> result =
                tagRepository.findAll(TagSpecifications.forStore(storeId, target, query), pageable);
        List<TagView> data = withUsage(target, result.getContent());

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new TagPage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public TagView getByPublicId(Long storeId, TagTarget target, String publicId) {
        return withUsage(target, List.of(requireOwned(storeId, target, publicId))).getFirst();
    }

    @Override
    @Transactional
    public TagView create(Long storeId, TagTarget target, Tag tag) {
        TagEntity entity = new TagEntity();
        entity.setStoreId(storeId);
        entity.setTarget(target);
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.TAG_PREFIX));
        String name = TagName.normalize(tag.name(), FIELD_NAME);
        entity.setName(name);
        entity.setColor(tag.color() != null
                ? TagColor.normalize(tag.color(), FIELD_COLOR)
                : TagPalette.pick(TagName.key(name)));
        entity.setStatus(tag.status() != null ? tag.status() : TagStatus.ACTIVE);

        TagEntity saved = save(entity, storeId, target);
        log.info("Tag created storeId={} target={} tagId={}", storeId, target, saved.getPublicId());
        return tagEntityMapper.toView(saved, 0L);
    }

    @Override
    @Transactional
    public void update(Long storeId, TagTarget target, String publicId, Tag patch) {
        TagEntity entity = requireOwned(storeId, target, publicId);
        if (patch.name() != null) {
            entity.setName(TagName.normalize(patch.name(), FIELD_NAME));
        }
        if (patch.color() != null) {
            entity.setColor(TagColor.normalize(patch.color(), FIELD_COLOR));
        }
        if (patch.status() != null) {
            entity.setStatus(patch.status());
        }
        save(entity, storeId, target);
        log.info("Tag updated storeId={} target={} tagId={}", storeId, target, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, TagTarget target, String publicId) {
        TagEntity entity = requireOwned(storeId, target, publicId);
        long detached = linkStore(target).countByTagIds(List.of(entity.getId()))
                .getOrDefault(entity.getId(), 0L);

        tagRepository.delete(entity);
        log.info("Tag deleted storeId={} target={} tagId={} detachedOwners={}",
                storeId, target, publicId, detached);
    }

    @Override
    @Transactional
    public TagView merge(Long storeId, TagTarget target, String targetPublicId, List<String> sourcePublicIds) {
        TagEntity survivor = requireOwned(storeId, target, targetPublicId);

        Set<String> sources = new LinkedHashSet<>(sourcePublicIds);
        if (sources.contains(targetPublicId)) {
            log.warn("Tag merge rejected storeId={} target={} tagId={} reason=target_among_sources",
                    storeId, target, targetPublicId);
            throw new InvalidTagRequestException(FIELD_SOURCE_IDS, "A tag cannot be merged into itself");
        }

        List<TagEntity> merged = tagRepository.findByStoreIdAndTargetAndPublicIdIn(storeId, target, sources);
        if (merged.size() != sources.size()) {
            throw new NotFoundException("Tag not found among the merge sources");
        }

        List<Long> sourceIds = merged.stream().map(TagEntity::getId).toList();
        TagLinkStore links = linkStore(target);
        int moved = links.repointToTag(sourceIds, survivor.getId());
        links.deleteByTagIds(sourceIds);
        tagRepository.deleteAll(merged);

        log.info("Tags merged storeId={} target={} tagId={} sources={} movedOwners={}",
                storeId, target, targetPublicId, sourceIds.size(), moved);
        return getByPublicId(storeId, target, targetPublicId);
    }

    @Override
    public List<TagView> listFor(Long storeId, TagTarget target, Long ownerId) {
        return listByOwner(storeId, target, List.of(ownerId)).getOrDefault(ownerId, List.of());
    }

    @Override
    public Map<Long, List<TagView>> listByOwner(Long storeId, TagTarget target, Collection<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        List<TagLink> links = linkStore(target).findByOwnerIds(ownerIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, TagView> tags = tagRepository
                .findAllById(links.stream().map(TagLink::tagId).distinct().toList()).stream()
                .filter(entity -> entity.getStoreId().equals(storeId))
                .collect(Collectors.toMap(TagEntity::getId, tagEntityMapper::toView));

        Map<Long, List<TagView>> byOwner = new LinkedHashMap<>();
        for (TagLink link : links) {
            TagView view = tags.get(link.tagId());
            if (view != null) {
                byOwner.computeIfAbsent(link.ownerId(), key -> new ArrayList<>()).add(view);
            }
        }
        byOwner.values().forEach(list -> list.sort(java.util.Comparator.comparing(TagView::name)));
        return byOwner;
    }

    @Override
    @Transactional
    public List<TagView> replaceFor(Long storeId, TagTarget target, Long ownerId, List<TagRef> tags) {
        apply(storeId, target, List.of(ownerId), tags, TagBulkMode.REPLACE);
        return listFor(storeId, target, ownerId);
    }

    @Override
    @Transactional
    public int applyBulk(Long storeId, TagTarget target, List<Long> ownerIds, List<TagRef> tags,
                         TagBulkMode mode) {
        return apply(storeId, target, ownerIds, tags, mode);
    }

    @Override
    public List<Long> resolveFilter(Long storeId, TagTarget target, List<String> tagPublicIds) {
        Set<String> requested = new LinkedHashSet<>(tagPublicIds);
        List<TagEntity> found = tagRepository.findByStoreIdAndTargetAndPublicIdIn(storeId, target, requested);
        if (found.size() != requested.size()) {
            throw new NotFoundException("Tag not found");
        }
        return found.stream().map(TagEntity::getId).toList();
    }

    /**
     * Resolves the submission once for every owner, then writes the per-owner difference. Names are
     * minted here, except when removing: an unknown name has nothing to detach, so creating it
     * would leave the seller with a tag he never asked for.
     */
    private int apply(Long storeId, TagTarget target, List<Long> ownerIds, List<TagRef> tags,
                      TagBulkMode mode) {
        List<Long> tagIds = resolve(storeId, target, tags, mode != TagBulkMode.REMOVE);
        TagLinkStore links = linkStore(target);

        Map<Long, List<Long>> current = links.findByOwnerIds(ownerIds).stream()
                .collect(Collectors.groupingBy(TagLink::ownerId,
                        Collectors.mapping(TagLink::tagId, Collectors.toList())));

        int updated = 0;
        for (Long ownerId : ownerIds) {
            TagAssignment plan = TagAssignment.plan(current.getOrDefault(ownerId, List.of()), tagIds, mode);
            if (plan.isEmpty()) {
                continue;
            }
            links.unlink(ownerId, plan.removed());
            links.link(ownerId, plan.added());
            updated++;
        }

        log.info("Tags applied storeId={} target={} mode={} owners={} updatedOwners={}",
                storeId, target, mode, ownerIds.size(), updated);
        return updated;
    }

    /** Turns the submitted entries into tag ids, minting the names the store does not own yet. */
    private List<Long> resolve(Long storeId, TagTarget target, List<TagRef> refs, boolean mintMissing) {
        if (refs.isEmpty()) {
            return List.of();
        }
        List<Tag> stored = tagEntityMapper.toDomains(
                tagRepository.findByStoreIdAndTargetOrderByNameAsc(storeId, target));
        TagResolution plan = TagResolution.plan(refs, stored);

        List<Long> resolved = new ArrayList<>(plan.existing().stream().map(Tag::id).toList());
        if (!mintMissing || plan.newNames().isEmpty()) {
            return resolved;
        }

        Instant now = Instant.now();
        for (String name : plan.newNames()) {
            tagRepository.insertIfAbsent(PublicIdGenerator.generate(PublicIdGenerator.TAG_PREFIX),
                    storeId, target.name(), name, TagPalette.pick(TagName.key(name)),
                    TagStatus.ACTIVE.name(), now);
        }
        List<String> keys = plan.newNames().stream().map(TagName::key).toList();
        List<TagEntity> minted = tagRepository.findByStoreIdAndTargetAndNameKeyIn(storeId, target, keys);
        minted.forEach(entity -> resolved.add(entity.getId()));

        log.info("Tags created storeId={} target={} tags={}", storeId, target, minted.size());
        return resolved;
    }

    private TagEntity save(TagEntity entity, Long storeId, TagTarget target) {
        try {
            return tagRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException violation) {
            if (UNIQUE_NAME_INDEX.equals(ConstraintViolations.nameOf(violation))) {
                log.warn("Tag rejected storeId={} target={} reason=duplicate_name", storeId, target);
                throw new TagAlreadyExistsException(FIELD_NAME,
                        "Tag with this name already exists; merge the two instead of renaming");
            }
            throw violation;
        }
    }

    private List<TagView> withUsage(TagTarget target, List<TagEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> usages = linkStore(target)
                .countByTagIds(entities.stream().map(TagEntity::getId).toList());
        return entities.stream()
                .map(entity -> tagEntityMapper.toView(entity, usages.getOrDefault(entity.getId(), 0L)))
                .toList();
    }

    private TagEntity requireOwned(Long storeId, TagTarget target, String publicId) {
        return tagRepository.findByStoreIdAndTargetAndPublicId(storeId, target, publicId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));
    }

    private TagLinkStore linkStore(TagTarget target) {
        TagLinkStore store = linkStores.get(target);
        if (store == null) {
            throw new InvalidTagRequestException("target", "Tags are not supported for " + target);
        }
        return store;
    }
}
